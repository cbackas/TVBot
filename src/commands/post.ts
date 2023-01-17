import { Channel, ChannelManager, ChannelType, ChatInputCommandInteraction, Collection, PermissionFlagsBits, SlashCommandBuilder, TextBasedChannel, ThreadChannel } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { createNewSubscription, updateEpisodes } from '../lib/shows'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'
import { Series } from '../interfaces/tvdb'
import { isForumChannel } from '../interfaces/discord'
import { buildShowEmbed } from '../lib/messages'

type SeriesWrapper = {
  series: Series
  post?: ThreadChannel
}

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName('post')
      .setDescription('Create a forum post for a show. Require "Manage Channels" permission.')
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
      .addStringOption(option => option.setName('imdb_id')
        .setDescription('The IMDB ID to search for')
        .setMinLength(9)
        .setRequired(true)
      )
      .addChannelOption(option => option.setName("forum")
        .setDescription('Destination Discord forum for show post (defaults to value defined in `/setting tv_forum`)')
        .addChannelTypes(ChannelType.GuildForum)
        .setRequired(false))
  },

  async execute(app: App, interaction: ChatInputCommandInteraction) {
    let imdbIds = interaction.options.getString('imdb_id', true).split(',')
    const forumInput = interaction.options.getChannel('forum', false)

    const progress = new ProgressMessageBuilder(interaction)
      .addStep(`Checking for existing forum posts with ID(s) \`${imdbIds}\``)
      .addStep(`Fetching show data`)
      .addStep('Creating forum post')
      .addStep('Saving show to DB')
      .addStep('Fetching upcoming episodes')

    try {
      // if the user passed in a forum then send the post to that forum
      const useInputForum = forumInput !== null && isForumChannel(forumInput as Channel)
      const tvForum = useInputForum ? forumInput.id : await getDefaultTVForumId(app)

      await progress.sendNextStep() // start step 1

      let messages: string[] = []

      for (const imdbId of imdbIds) {
        const existingPosts = await checkForExistingPosts(imdbId, tvForum) ?? []
        if (existingPosts.length > 0) {
          messages.push(`A post for \`${imdbId}\` already exists `)
          imdbIds = imdbIds.filter(id => id !== imdbId)
        }
      }

      if (imdbIds.length == 0) {
        throw new ProgressError(`All show(s) already have a post in <#${tvForum}>`)
      }

      await progress.sendNextStep() // start step 2

      const seriesList = new Collection<string, SeriesWrapper>()

      for (const imdbId of imdbIds) {
        const tvdbSeries = await getSeriesByImdbId(imdbId)
        if (tvdbSeries) {
          seriesList.set(imdbId, {
            series: tvdbSeries,
          })
        }
      }

      if (seriesList.size == 0) {
        throw new ProgressError(`No show found with IMDB ID(s) ${imdbIds}`)
      }

      await progress.sendNextStep() // start step 3

      for (const [imdbId, series] of seriesList) {
        try {
          const newPost = await createForumPost(interaction.client.channels, series.series, tvForum)
          seriesList.set(imdbId, {
            series: series.series,
            post: newPost,
          })
        } catch (error) {
          messages.push(`Error creating post for \`${imdbId}\``)
          seriesList.delete(imdbId)
        }
      }

      if (seriesList.size == 0) {
        throw new ProgressError(`Error creating posts for \`${imdbIds}\``)
      }

      await progress.sendNextStep() // start step 4

      for (const [imdbId, series] of seriesList) {
        const { series: tvDBSeries, post } = series

        if (!post) continue

        const show = await saveShowToDB(imdbId, tvDBSeries.id, tvDBSeries.name, post as TextBasedChannel)

        await post.send({
          embeds: [await buildShowEmbed(imdbId, tvDBSeries, show.destinations)],
        })

        await updateEpisodes(show.imdbId, show.tvdbId, series.series)

        messages.push(`Created post for \`${tvDBSeries.name}\` (${imdbId}) - <#${post.id}>`)
        console.info(`Added show ${tvDBSeries.name} (${imdbId})`)
      }

      await progress.sendNextStep() // start step 5

      await scheduleAiringMessages(app)

      return await progress.sendNextStep(`Creating post(s) in <#${tvForum}>:\n\n${messages.join('\n')}`) // finish step 5
    } catch (error) {
      // catch our custom error and display it for the user
      if (error instanceof ProgressError) {
        const message = `${progress.toString()}\n\nError: ${error.message}`
        return await interaction.editReply(message)
      }

      throw error
    }
  }
}

/**
 * Fetch the default TV forum channel ID from the database or throw a ProgressError if it's not set
 * @param app main application object instance
 * @returns ID of the default TV forum
 */
const getDefaultTVForumId = async (app: App) => {
  const forumId = app.getSettings()?.defaultForum

  if (!forumId) {
    throw new ProgressError('No TV forum configured, use /settings tv_forum <channel> to set the default TV forum')
  }

  return forumId
}

/**
 * Checks the database for ShowDestinations with the given IMDB ID and forumId and returns them if they exist
 * @param imdbId imdb id to search for
 * @param tvForum id of the discord forum to check for existing posts 
 */
const checkForExistingPosts = async (imdbId: string, tvForum: string) => {
  const show = await client.show.findFirst({
    where: {
      imdbId,
      destinations: {
        some: {
          forumId: tvForum
        }
      }
    },
    select: {
      name: true,
      destinations: true
    }
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
const createForumPost = async (channels: ChannelManager, tvdbSeries: Series, tvForumId: string): Promise<ThreadChannel<boolean>> => {
  const forumChannel = await channels.fetch(tvForumId)

  if (forumChannel == null || !isForumChannel(forumChannel)) {
    throw new ProgressError('No tv forum found')
  }

  // create the forum post
  return await forumChannel.threads.create({
    name: tvdbSeries.name,
    autoArchiveDuration: 10080,
    message: {
      content: `${tvdbSeries.image}`
    }
  })
}

/**
 * wrapper function to save a show to the database
 * @param imdbId imdb id of the show to save
 * @param tvdbSeriesId tvdb id of the show to save
 * @param seriesName name of the show
 * @param channel channel to save as the destination
 */
const saveShowToDB = async (imdbId: string, tvdbSeriesId: number, seriesName: string, channel: TextBasedChannel) => {
  try {
    return await createNewSubscription(imdbId, tvdbSeriesId, seriesName, channel)
  } catch (error) {
    console.error(error)
    throw new ProgressError('Something went wrong saving the show to the DB')
  }
} 
