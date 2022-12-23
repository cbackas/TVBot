import { CacheType, ChannelManager, ChatInputCommandInteraction, PermissionFlagsBits, SlashCommandBuilder, ThreadChannel } from 'discord.js'
import client, { DBChannelType } from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { Prisma } from '@prisma/client'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { updateDBEpisodes } from '../lib/dbManager'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'
import { Series } from '../interfaces/tvdb'
import { isThreadChannel } from '../interfaces/discord'
import { isForumChannel } from '../interfaces/discord'

/**
 * Slash command definition for `/post`
 */
const slashCommand = new SlashCommandBuilder()
  .setName('post')
  .setDescription('Create a forum post for a show. Require "Manage Channels" permission.')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
  .addStringOption(option => option.setName('imdb_id')
    .setDescription('The IMDB ID to search for')
    .setMinLength(9)
    .setRequired(true)
  )

/**
 * The main execution method for the `/post` command
 * @param app main application object instance
 * @param interaction the discord interaction that triggered the command
 * @returns nothing important
 */
const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  const imdbId = interaction.options.getString('imdb_id', true)

  const progress = new ProgressMessageBuilder()
    .addStep(`Checking for existing forum posts with ID \`${imdbId}\``)
    .addStep(`Fetching show data`)
    .addStep('Creating forum post')
    .addStep('Saving show to DB')
    .addStep('Fetching upcoming episodes')

  // /**
  //  * Wrapper function that updates the ProgressMessage object and sends it to the user
  //  * @param message optional message to append to the progress message
  //  * @returns the sent discord message
  //  */
  const nextStep = async (message?: string) => await interaction.editReply(progress.nextStep() + message ?? '')

  try {
    await nextStep() // start step 1

    const tvForum = await getDefaultTVForumId(app)

    await checkForExistingPosts(interaction.client.channels, imdbId, tvForum)

    await nextStep() // start step 2

    const tvdbSeries = await getSeriesByImdbId(imdbId)

    if (!tvdbSeries) {
      throw new ProgressError(`No show found with IMDB ID ${imdbId}`)
    }

    await nextStep() // start step 3

    const newPost = await createForumPost(interaction.client.channels, tvdbSeries, tvForum)

    await nextStep() // start step 4

    const newShow = await saveShowToDB(tvdbSeries, imdbId, newPost.id, tvForum)

    await nextStep() // start step 5

    await updateDBEpisodes(newShow)
    await scheduleAiringMessages(app)

    console.log(`Added show ${tvdbSeries.name} (${imdbId})`)
    // finish step 5
    return await nextStep(`Created post <#${newPost.id}>`)
  } catch (error) {
    // catch our custom error and display it for the user
    if (error instanceof ProgressError) {
      const message = `${progress.toString()}\n\nError: ${error.message}`
      return await interaction.editReply(message)
    }

    throw error
  }
}

export const command: CommandV2 = {
  slashCommand: {
    main: slashCommand
  },
  execute
}

/**
 * Fetch the default TV forum channel ID from the database or throw a ProgressError if it's not set
 * @param app main application object instance
 * @returns ID of the default TV forum
 */
const getDefaultTVForumId = async (app: App) => {
  const forumId = app.getSettings().find(s => s.key === 'tv_forum')?.value

  if (!forumId) {
    throw new ProgressError('No TV forum configured, use /settings tv_forum <channel> to set the default TV forum')
  }

  return forumId
}

/**
 * Checks the database for ShowDestinations with the given IMDB ID and forumId and throws a ProgressError if one is found
 * @param channels discordjs ChannelManager to fetch channels from
 * @param imdbId imdb id to search for
 * @param tvForum id of the discord forum to check for existing posts 
 */
const checkForExistingPosts = async (channels: ChannelManager, imdbId: string, tvForum: string) => {
  const existingShowDestinations = await client.showDestination.findMany({
    where: {
      forumId: tvForum,
      channelType: DBChannelType.FORUM,
      show: {
        imdbId: imdbId
      },
    },
    select: {
      showId: true,
      channelId: true,
      show: {
        select: {
          id: true,
          name: true,
          imdbId: true,
          tvdbId: true
        }
      }
    }
  })

  // if we found a show destination, fetch the channel and throw an error
  if (existingShowDestinations.length > 0) {
    const channel = await channels.fetch(existingShowDestinations[0].channelId)

    if (channel == null || !isThreadChannel(channel)) {
      // todo run show cleanup function?
      throw new ProgressError('A forum post already exists for that show, but the channel could not be found. This error shouldn\'t ever really happen. Probably.')
    }

    throw new ProgressError(`A forum post already exists for that show <#${channel.id}>`)
  }
}

/**
 * Creates a discord forum post in the given forum for the given tvdb series
 * @param channels discordjs ChannelManager to fetch channels from
 * @param tvdbSeries series to create the post for
 * @param tvForumId discord forum to create a post in
 * @returns 
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
 * Saves a show to the database and creates a ShowDestination, associating the show with a discord channel
 * @param tvdbSeries series to save
 * @param imdbId imdb id of the show
 * @param newPostId id of the discord post to save
 * @param forumId id of the discord forum the post is in
 * @returns 
 */
const saveShowToDB = async (tvdbSeries: Series, imdbId: string, newPostId: string, forumId: string) => {
  try {
    const data = Prisma.validator<Prisma.ShowCreateInput>()({
      name: tvdbSeries.name,
      imdbId: imdbId,
      tvdbId: tvdbSeries.id,
      ShowDestination: {
        create: {
          channelId: newPostId,
          forumId: forumId,
          channelType: DBChannelType.FORUM,
        }
      }
    })

    return await client.show.create({ data })
  } catch (error) {
    // if the show already exists, throw a custom error
    if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
      throw new ProgressError('Show already exists')
    }

    console.error(error)
    throw new ProgressError('Something went wrong saving the show to the DB')
  }
} 
