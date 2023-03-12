import { ChannelType, type ChatInputCommandInteraction, Collection, PermissionFlagsBits, SlashCommandBuilder, type SlashCommandStringOption, SlashCommandSubcommandBuilder, type TextBasedChannel } from 'discord.js'
import client from '../lib/prisma'
import { type CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { type App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { createNewSubscription, updateEpisodes } from '../lib/shows'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'
import { buildShowEmbed } from '../lib/messages'
import { type SeriesExtendedRecord } from '../interfaces/tvdb.generated'
import { parseIMDBIds } from '../lib/util'

/**
 * Standardized slash command option for getting IMDB ID
 * @param option string option callback parameter
 * @returns string option with options set
 */
const imdbOption = (option: SlashCommandStringOption): SlashCommandStringOption => {
  return option.setName('imdb_id')
    .setDescription('The IMDB ID to search for')
    .setMinLength(9)
    .setRequired(true)
}

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
  async executeCommand (app: App, interaction: ChatInputCommandInteraction) {
    const imdbIds = parseIMDBIds(interaction.options.getString('imdb_id', true))

    if (imdbIds.length >= 10) {
      return await interaction.editReply({
        content: 'You can only create 10 posts at a time'
      })
    }

    const imdbIdString = imdbIds.map(s => `\`${s}\``).join(', ')

    const subCommand = interaction.options.getSubcommand()
    if (subCommand === '') return await interaction.editReply('Invalid subcommand')

    const progress = new ProgressMessageBuilder(interaction)
      .addStep('Check for existing show subscription')
      .addStep(`Searching for show with IMDB ID(s) ${imdbIdString}`)
      .addStep('Linking show to channel in database')
      .addStep('Fetching upcoming episodes')

    let channel: TextBasedChannel | undefined

    // the `here` subcommand links the show to the current channel
    if (subCommand === 'here' && interaction.channel !== null) {
      channel = interaction.channel
    }

    // the `channel` subcommand allows a user to specify a text channel
    if (subCommand === 'channel') {
      channel = interaction.options.getChannel('channel', true) as TextBasedChannel
    }

    // error if the channel didnt get set for some reason
    if (channel == null || channel.type === ChannelType.GuildStageVoice) {
      return await interaction.editReply('Invalid channel')
    }

    try {
      await progress.sendNextStep() // start step 1

      const seriesList = new Collection<string, SeriesExtendedRecord>()

      for (const imdbId of imdbIds) {
        const series = await getSeriesByImdbId(imdbId)
        if (series !== undefined) seriesList.set(imdbId, series)
      }

      if (seriesList.size === 0) {
        throw new ProgressError(`No shows found with IMDB ID(s) ${imdbIdString}`)
      }

      await progress.sendNextStep() // start step 2

      const messages: string[] = []

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
          console.info(`[New Subscription] ${tvdbSeries.name} (${imdbId}) [${channel.id}`)
          const show = await createNewSubscription(imdbId, tvdbSeries.id, tvdbSeries.name, channel)
          await updateEpisodes(show.imdbId, show.tvdbId, tvdbSeries)

          await channel.send({
            content: `Linked \`${tvdbSeries.name}\` to <#${channel.id}>`,
            embeds: [await buildShowEmbed(imdbId, tvdbSeries, show.destinations)]
          })

          messages.push(`Linked show \`${tvdbSeries.name}\` (${imdbId})`)
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
async function checkForExistingSubscription (imdbId: string, channelId: string): Promise<boolean> {
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

  const { destinations } = show

  // if the show is in the DB but has no destinations then we can just return
  if (destinations.length <= 0) return false

  // check if the show is already linked to the channel
  const existingDestination = destinations.find(d => d.channelId === channelId)

  if (existingDestination === undefined) return false

  // if the show is already linked to the channel
  return true
}
