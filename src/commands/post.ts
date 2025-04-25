import {
  type APIEmbed,
  type Channel,
  ChannelType,
  type ChatInputCommandInteraction,
  Collection,
  PermissionFlagsBits,
  SlashCommandBuilder,
  type TextBasedChannel,
  type ThreadChannel,
} from "npm:discord.js"
import client from "lib/prisma.ts"
import { type CommandV2 } from "interfaces/command.ts"
import { ProgressMessageBuilder } from "lib/progressMessages.ts"
import { getSeriesByImdbId } from "lib/tvdb.ts"
import { createNewSubscription, updateEpisodes } from "lib/shows.ts"
import { ProgressError } from "interfaces/error.ts"
import { isForumChannel } from "interfaces/discord.ts"
import { buildShowEmbed } from "lib/messages.ts"
import { type SeriesExtendedRecord } from "interfaces/tvdb.generated.ts"
import { type Destination, type Show } from "prisma-client/client.ts"
import { parseIMDBIds } from "lib/util.ts"
import { Settings } from "lib/settingsManager.ts"

interface SeriesWrapper {
  series: SeriesExtendedRecord
  post?: ThreadChannel
}

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("post")
      .setDescription(
        'Create a forum post for a show. Require "Manage Channels" permission.',
      )
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
      .addStringOption((option) =>
        option.setName("imdb_id")
          .setDescription("The IMDB ID to search for")
          .setMinLength(9)
          .setRequired(true)
      )
      .addChannelOption((option) =>
        option.setName("forum")
          .setDescription(
            "Destination Discord forum for show post (defaults to value defined in `/setting tv_forum`)",
          )
          .addChannelTypes(ChannelType.GuildForum)
          .setRequired(false)
      ),
  },

  async executeCommand(interaction: ChatInputCommandInteraction) {
    let imdbIds = parseIMDBIds(interaction.options.getString("imdb_id", true))

    if (imdbIds.length > 10) {
      return await interaction.editReply({
        content: "You can only create 10 posts at a time",
      })
    }

    const imdbIdString = imdbIds.map((s) => `\`${s}\``).join(", ")

    const forumInput = interaction.options.getChannel("forum", false)

    const progress = new ProgressMessageBuilder(interaction)
      .addStep(`Checking for existing forum posts with ID(s) ${imdbIdString}`)
      .addStep("Fetching show data")
      .addStep("Creating forum post")
      .addStep("Saving show to DB")
      .addStep("Fetching upcoming episodes")

    try {
      // if the user passed in a forum then send the post to that forum
      const useInputForum = forumInput !== null &&
        isForumChannel(forumInput as Channel)
      const tvForum = useInputForum ? forumInput.id : getDefaultTVForumId()

      await progress.sendNextStep() // start step 1

      const messages: string[] = []

      for (const imdbId of imdbIds) {
        const existingPosts = await checkForExistingPosts(imdbId, tvForum) ?? []
        if (existingPosts.length > 0) {
          messages.push(`A post for \`${imdbId}\` already exists `)
          imdbIds = imdbIds.filter((id) => id !== imdbId)
        }
      }

      if (imdbIds.length === 0) {
        throw new ProgressError(
          `All show(s) already have a post in <#${tvForum}>`,
        )
      }

      await progress.sendNextStep() // start step 2

      const seriesList = new Collection<string, SeriesWrapper>()

      for (const imdbId of imdbIds) {
        const tvdbSeries = await getSeriesByImdbId(imdbId)
        if (tvdbSeries != null) {
          seriesList.set(imdbId, {
            series: tvdbSeries,
          })
        }
      }

      if (seriesList.size === 0) {
        throw new ProgressError(`No show found with IMDB ID(s) ${imdbIdString}`)
      }

      await progress.sendNextStep() // start step 3

      for (const [imdbId, series] of seriesList) {
        const embed = buildShowEmbed(imdbId, series.series)

        try {
          const forumChannel = await interaction.client.channels.fetch(tvForum)
          const newPost = await createForumPost(
            forumChannel,
            embed,
            series.series.name,
          )
          seriesList.set(imdbId, {
            series: series.series,
            post: newPost,
          })
        } catch (error) {
          messages.push(`Error creating post for \`${imdbId}\``)
          seriesList.delete(imdbId)
        }
      }

      if (seriesList.size === 0) {
        throw new ProgressError(`Error creating posts for ${imdbIdString}`)
      }

      await progress.sendNextStep() // start step 4

      for (const [imdbId, series] of seriesList) {
        const { series: tvDBSeries, post } = series

        if (post == null) continue

        const show = await saveShowToDB(
          imdbId,
          tvDBSeries.id,
          tvDBSeries.name,
          post as TextBasedChannel,
        )

        await updateEpisodes(show.imdbId, show.tvdbId, series.series)

        messages.push(
          `Created post for \`${tvDBSeries.name}\` (${imdbId}) - <#${post.id}>`,
        )
        console.info(`Added show ${tvDBSeries.name} (${imdbId})`)
      }

      await progress.sendNextStep() // start step 5

      return await progress.sendNextStep(
        `Creating post(s) in <#${tvForum}>:\n\n${messages.join("\n")}`,
      ) // finish step 5
    } catch (error) {
      // catch our custom error and display it for the user
      if (error instanceof ProgressError) {
        const message = `${progress.toString()}\n\nError: ${error.message}`
        return await interaction.editReply(message)
      }

      throw error
    }
  },
}

/**
 * Fetch the default TV forum channel ID from the database or throw a ProgressError if it's not set
 * @param app main application object instance
 * @returns ID of the default TV forum
 */
function getDefaultTVForumId(): string {
  const forumId = Settings.getInstance().fetch()?.defaultForum
  if (forumId == null) {
    throw new ProgressError(
      "No TV forum configured, use /settings tv_forum <channel> to set the default TV forum",
    )
  }

  return forumId
}

/**
 * Checks the database for ShowDestinations with the given IMDB ID and forumId and returns them if they exist
 * @param imdbId imdb id to search for
 * @param tvForum id of the discord forum to check for existing posts
 */
async function checkForExistingPosts(
  imdbId: string,
  tvForum: string,
): Promise<Destination[] | undefined> {
  const show = await client.show.findFirst({
    where: {
      imdbId,
      destinations: {
        some: {
          forumId: tvForum,
        },
      },
    },
    select: {
      name: true,
      destinations: true,
    },
  })

  // if the show isnt in the DB then we can just return
  if (show === null) return undefined
  // if the show is in the DB but has no destinations then we can just return
  if (show.destinations.length === 0) return undefined

  return show.destinations
}

/**
 * Creates a discord forum post in the given forum for the given tvdb series
 * @param channels discordjs ChannelManager to fetch channels from
 * @param tvdbSeries series to create the post for
 * @param tvForumId discord forum to create a post in
 * @returns the created forum thread
 */
async function createForumPost(
  channel: Channel | null,
  embed: APIEmbed,
  seriesName: string,
): Promise<ThreadChannel<boolean>> {
  if (channel == null || !isForumChannel(channel)) {
    throw new ProgressError("No tv forum found")
  }

  // create the forum post
  const result = await channel.threads.create({
    name: seriesName,
    autoArchiveDuration: 10080,
    message: {
      embeds: [embed],
    },
  })

  await result.lastMessage?.pin()

  return result
}

/**
 * wrapper function to save a show to the database
 * @param imdbId imdb id of the show to save
 * @param tvdbSeriesId tvdb id of the show to save
 * @param seriesName name of the show
 * @param channel channel to save as the destination
 */
async function saveShowToDB(
  imdbId: string,
  tvdbSeriesId: number,
  seriesName: string,
  channel: TextBasedChannel,
): Promise<Show> {
  try {
    console.info(`[New Subscription] ${seriesName} (${imdbId}) ${channel.id}`)
    return await createNewSubscription(
      imdbId,
      tvdbSeriesId,
      seriesName,
      channel,
    )
  } catch (error) {
    console.error(error)
    throw new ProgressError("Something went wrong saving the show to the DB")
  }
}
