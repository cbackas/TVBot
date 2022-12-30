import { CacheType, ChannelType, ChatInputCommandInteraction, PermissionFlagsBits, SlashCommandBuilder, SlashCommandStringOption, SlashCommandSubcommandBuilder, TextBasedChannel } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { createNewSubscription, updateEpisodes } from '../lib/database/shows'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'

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

/**
 * The main execution method for the `/link` command
 * @param app main application object instance
 * @param interaction the discord interaction that triggered the command
 * @returns nothing important
 */
const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  const imdbId = interaction.options.getString('imdb_id', true)

  const subCommand = interaction.options.getSubcommand()
  if (!subCommand) return await interaction.editReply('Invalid subcommand')

  const progress = new ProgressMessageBuilder()
    .addStep('Check for existing show subscription')
    .addStep(`Searching for show with IMDB ID \`${imdbId}\``)
    .addStep('Linking show to channel in database')
    .addStep('Fetching upcoming episodes')

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
  const nextStep = async (message?: string) => await interaction.editReply(progress.nextStep() + message ?? '')

  try {
    await nextStep() // start step 1

    const tvdbSeries = await getSeriesByImdbId(imdbId)

    if (tvdbSeries === undefined) {
      throw new ProgressError(`No show found with IMDB ID \`${imdbId}\``)
    }

    await nextStep() // start step 2

    await checkForExistingSubscription(imdbId, channel.id)

    await nextStep() // start step 3

    const show = await createNewSubscription(imdbId, tvdbSeries.id, tvdbSeries.name, channel)

    await nextStep() // start step 4

    await updateEpisodes(show.imdbId, show.tvdbId)
    await scheduleAiringMessages(app)

    console.log(`Added show ${tvdbSeries.name} (${imdbId})`)
    return await nextStep(`Linked show \`${tvdbSeries.name}\` to <#${channel.id}>`)
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
    main: slashCommand,
    subCommands: [hereSubCommand, channelSubCommand]
  },
  execute
}

/**
 * Check for existing show-channel subscriptions in the DB and throws a ProgressError if one is found
 * @param imdbId imdb id to check for
 * @param channelId discord channel id to check for
 */
const checkForExistingSubscription = async (imdbId: string, channelId: string): Promise<void> => {
  const show = await client.show.findUnique({
    where: {
      imdbId
    },
    select: {
      name: true,
      destinations: true
    }
  })

  // if the show isnt in the DB then we can just return
  if (show === null) return

  const { name, destinations } = show

  // if the show is in the DB but has no destinations then we can just return
  if (destinations.length <= 0) return

  // check if the show is already linked to the channel
  const existingDestination = destinations.find(d => d.channelId === channelId)

  if (existingDestination === undefined) return

  // if the show is already linked to the channel then throw an error
  throw new ProgressError(`Show \`${name}\` is already linked to <#${channelId}>`)
}
