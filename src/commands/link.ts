import { ChannelType, ChatInputCommandInteraction, Collection, PermissionFlagsBits, SlashCommandBuilder, SlashCommandStringOption, SlashCommandSubcommandBuilder, TextBasedChannel } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { createNewSubscription, updateEpisodes } from '../lib/shows'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'
import { Series } from '../interfaces/tvdb'

/**
 * Standardized slash command option for getting IMDB ID
 * @param option string option callback parameter
 * @returns string option with options set
 */
const imdbOption = (option: SlashCommandStringOption) => option.setName('imdb_id')
  .setDescription('The IMDB ID to search for')
  .setMinLength(9)
  .setRequired(true)

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName('link')
      .setDescription('Link a show to a channel for notifications.')
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName('here')
        .setDescription('Link a show to the current channel for notifications.')
        .addStringOption(imdbOption),
      new SlashCommandSubcommandBuilder()
        .setName('channel')
        .setDescription('Link a show to a channel for notifications.')
        .addChannelOption(option => option.setName('channel')
          .setDescription('The channel to announce episodes in')
          .addChannelTypes(ChannelType.GuildText)
          .setRequired(true))
        .addStringOption(imdbOption)
    ]
  },
  async execute(app: App, interaction: ChatInputCommandInteraction) {
    const imdbIds = interaction.options.getString('imdb_id', true).split(',')

    const subCommand = interaction.options.getSubcommand()
    if (!subCommand) return await interaction.editReply('Invalid subcommand')

    const progress = new ProgressMessageBuilder(interaction)
      .addStep('Check for existing show subscription')
      .addStep(`Searching for show with IMDB ID(s) \`${imdbIds}\``)
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

    try {
      await progress.sendNextStep() // start step 1

      const seriesList = new Collection<string, Series>()

      for (const imdbId of imdbIds) {
        const series = await getSeriesByImdbId(imdbId)
        if (series !== undefined) seriesList.set(imdbId, series)
      }

      if (seriesList.size == 0) {
        throw new ProgressError(`No shows found with IMDB ID(s) \`${imdbIds}\``)
      }

      await progress.sendNextStep() // start step 2

      let messages: string[] = []

      for (const [imdbId, tvdbSeries] of seriesList) {
        const existingSubscription = await checkForExistingSubscription(imdbId, channel.id)
        if (existingSubscription) {
          messages.push(`Show \`${tvdbSeries.name}\` is already linked to <#${channel.id}>`)
          seriesList.delete(imdbId)
        }
      }

      await progress.sendNextStep() // start step 3

      for (const [imdbId, tvdbSeries] of seriesList) {
        try {
          const show = await createNewSubscription(imdbId, tvdbSeries.id, tvdbSeries.name, channel)
          await updateEpisodes(show.imdbId, show.tvdbId, tvdbSeries)
          messages.push(`Linked show \`${tvdbSeries.name}\` (${imdbId})`)
          console.info(`Added show ${tvdbSeries.name} (${imdbId})`)
        } catch (error) {
          messages.push(`Failed to link show \`${tvdbSeries.name}\` (${imdbId})`)
          console.error(error)
        }
      }

      await progress.sendNextStep() // start step 4

      await scheduleAiringMessages(app)

      return await progress.sendNextStep(`Linked show(s) to <#${channel.id}>:\n\n${messages.join('\n')}`)
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
 * Check for existing show-channel subscriptions in the DB and throws a ProgressError if one is found
 * @param imdbId imdb id to check for
 * @param channelId discord channel id to check for
 */
const checkForExistingSubscription = async (imdbId: string, channelId: string): Promise<boolean> => {
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
  if (show === null) return false

  const { name, destinations } = show

  // if the show is in the DB but has no destinations then we can just return
  if (destinations.length <= 0) return false

  // check if the show is already linked to the channel
  const existingDestination = destinations.find(d => d.channelId === channelId)

  if (existingDestination === undefined) return false

  // if the show is already linked to the channel
  return true
}
