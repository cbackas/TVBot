import { CacheType, ChannelType, ChatInputCommandInteraction, Message, PermissionFlagsBits, SlashCommandBuilder, SlashCommandStringOption, SlashCommandSubcommandBuilder, TextBasedChannel } from 'discord.js'
import client, { DBChannelType } from '../lib/prisma'
import { Command } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { updateDBEpisodes } from '../lib/dbManager'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'
import { isThreadChannel } from '../interfaces/discord'

type NextStep = (message?: string) => Promise<Message>

/**
 * Standardized slash command option for getting IMDB ID
 * @param option string option callback parameter
 * @returns string option with options set
 */
const imdbOption = (option: SlashCommandStringOption) => option.setName('imdb_id')
  .setDescription('The IMDB ID to search for')
  .setMinLength(9)
  .setRequired(true)

/**
 * The `/link here` subcommand definition
 */
const hereSubCommand = new SlashCommandSubcommandBuilder()
  .setName('here')
  .setDescription('Link a show to the current channel for notifications.')
  .addStringOption(imdbOption)

/**
 * The `/link channel` subcommand definition
 */
const channelSubCommand = new SlashCommandSubcommandBuilder()
  .setName('channel')
  .setDescription('Link a show to a channel for notifications.')
  .addChannelOption(option => option.setName('channel')
    .setDescription('The channel to announce episodes in')
    .addChannelTypes(ChannelType.GuildText)
    .setRequired(true))
  .addStringOption(imdbOption)

/**
 * The `/link` command definition
 */
const slashCommand = new SlashCommandBuilder()
  .setName('link')
  .setDescription('Link a show to a channel for notifications.')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
  .addSubcommand(hereSubCommand)
  .addSubcommand(channelSubCommand)

/**
 * The main execution method for the `/link` command
 * @param app main application object instance
 * @param interaction the discord interaction that triggered the command
 * @returns nothing important
 */
const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  const imdbId = interaction.options.getString('imdb_id', true)

  const progressMessage = new ProgressMessageBuilder()
    .addStep('Check for existing show subscription')
    .addStep(`Searching for show with IMDB ID \`${imdbId}\``)
    .addStep('Linking show to channel in database')
    .addStep('Fetching upcoming episodes')

  const subCommand = interaction.options.getSubcommand()

  let channel: TextBasedChannel | undefined

  // the `here` subcommand links the show to the current channel
  if (subCommand == 'here' && interaction.channel !== null) {
    channel = interaction.channel
  }

  // the `channel` subcommand allows a user to specify a text channel
  if (subCommand == 'channel') {
    channel = interaction.options.getChannel('channel', true) as TextBasedChannel
  }

  // error if the channel didnt get set for some reason
  if (channel === undefined) {
    return await interaction.editReply('Invalid channel')
  }

  /**
   * Wrapper function that updates the ProgressMessage object and sends it to the user
   * @param message optional message to append to the progress message
   * @returns the sent discord message
   */
  const nextStep: NextStep = async (message) => await interaction.editReply(progressMessage.nextStep() + message ?? '')

  try {
    await nextStep() // start step 1

    const tvdbSeries = await getSeriesByImdbId(imdbId)

    if (tvdbSeries === undefined) {
      throw new ProgressError(`No show found with IMDB ID \`${imdbId}\``)
    }

    await nextStep() // start step 2

    await checkForExistingSubscription(imdbId, channel.id, tvdbSeries.name)

    await nextStep() // start step 3

    const newDestination = await createNewSubscription(imdbId, channel, tvdbSeries.name, tvdbSeries.id)

    await nextStep() // start step 4

    await updateDBEpisodes(newDestination.show)
    await scheduleAiringMessages(app)

    console.log(`Added show ${tvdbSeries.name} (${imdbId})`)
    // return the final message
    return await nextStep(`Linked show \`${tvdbSeries.name}\` to <#${channel.id}>`)
  } catch (error) {
    // catch our custom error and display it for the user
    if (error instanceof ProgressError) {
      const message = `${progressMessage.toString()}\n\nError: ${error.message}`
      return await interaction.editReply(message)
    }

    throw error
  }
}

export const command: Command = {
  data: slashCommand,
  execute
}

/**
 * Check for existing show-channel subscriptions in the DB and throws a ProgressError if one is found
 * @param imdbId imdb id to check for
 * @param channelId discord channel id to check for
 * @param seriesName seriesName to use for response message if the show is already linked
 */
const checkForExistingSubscription = async (imdbId: string, channelId: string, seriesName: string): Promise<void> => {
  const existingDestination = await client.showDestination.findFirst({
    where: {
      show: {
        imdbId
      },
      channelId: channelId
    }
  })

  if (existingDestination !== null) {
    throw new ProgressError(`Show \`${seriesName}\` is already linked to ${channelId}`)
  }
}

/**
 * 
 * @param imdbId imdbID for the show to subscribe to
 * @param channel discord channel to send notifications to
 * @param seriesName name of the tv show
 * @param tvdbSeriesId tvdb id for the show
 * @returns 
 */
const createNewSubscription = async (imdbId: string, channel: TextBasedChannel, seriesName: string, tvdbSeriesId: number) => {
  const channelId = channel.id
  const channelType = isThreadChannel(channel) ? DBChannelType.FORUM : DBChannelType.TEXT

  // create a ShowDestination in the DB
  // also creates a Show in the DB if a matching show doesnt exist
  return await client.showDestination.create({
    data: {
      channelId: channelId,
      channelType: channelType,
      show: {
        connectOrCreate: {
          where: {
            imdbId: imdbId,
          },
          create: {
            imdbId: imdbId,
            tvdbId: tvdbSeriesId,
            name: seriesName,
          }
        }
      }
    },
    include: {
      show: true,
    }
  })
}
