import { CacheType, Channel, ChannelType, ChatInputCommandInteraction, ForumChannel, Message, PermissionFlagsBits, SlashCommandBuilder, SlashCommandStringOption, SlashCommandSubcommandBuilder, TextBasedChannel, TextChannel } from 'discord.js'
import client, { DBChannelType } from '../lib/prisma'
import { Command } from '../interfaces/command'
import { Prisma } from '@prisma/client'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { updateDBEpisodes } from '../lib/dbManager'
import { scheduleAiringMessages } from '../lib/episodeNotifier'
import { ProgressError } from '../interfaces/error'
import { isThreadChannel } from '../interfaces/discord'

type NextStep = (message?: string) => Promise<Message>

const imdbOption = (option: SlashCommandStringOption) => option.setName('imdb_id')
  .setDescription('The IMDB ID to search for')
  .setMinLength(9)
  .setRequired(true)


const hereSubCommand = new SlashCommandSubcommandBuilder()
  .setName('here')
  .setDescription('Link a show to the current channel for notifications.')
  .addStringOption(imdbOption)

const channelSubCommand = new SlashCommandSubcommandBuilder()
  .setName('channel')
  .setDescription('Link a show to a channel for notifications.')
  .addChannelOption(option => option.setName('channel')
    .setDescription('The channel to announce episodes in')
    .addChannelTypes(ChannelType.GuildText)
    .setRequired(true))
  .addStringOption(imdbOption)

const slashCommand = new SlashCommandBuilder()
  .setName('link')
  .setDescription('Link a show to a channel for notifications.')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
  .addSubcommand(hereSubCommand)
  .addSubcommand(channelSubCommand)

const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  const imdbId = interaction.options.getString('imdb_id', true)

  const progressMessage = new ProgressMessageBuilder()
    .addStep('Check for existing show subscription')
    .addStep(`Searching for show with IMDB ID \`${imdbId}\``)
    .addStep('Linking show to channel in database')
    .addStep('Fetching upcoming episodes')

  const subCommand = interaction.options.getSubcommand()

  let channel: TextBasedChannel | undefined

  if (subCommand == 'here' && interaction.channel !== null) {
    channel = interaction.channel
  }

  if (subCommand == 'channel') {
    channel = interaction.options.getChannel('channel', true) as TextBasedChannel
  }

  if (channel === undefined) {
    return await interaction.editReply('Invalid channel')
  }

  const nextStep: NextStep = async (message) => await interaction.editReply(progressMessage.nextStep() + message ?? '')

  try {
    return await addShow(app, nextStep, progressMessage, channel, imdbId)
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

const addShow = async (app: App, nextStep: NextStep, progressMessage: ProgressMessageBuilder, channel: TextBasedChannel, imdbId: string) => {
  await nextStep()

  const tvdbSeries = await getSeriesByImdbId(imdbId)

  if (tvdbSeries === undefined) {
    throw new ProgressError(`No show found with IMDB ID \`${imdbId}\``)
  }

  await nextStep()

  const existingDestination = await client.showDestination.findFirst({
    where: {
      show: {
        imdbId
      },
      channelId: channel.id
    }
  })

  if (existingDestination !== null) {
    throw new ProgressError(`Show \`${tvdbSeries.name}\` is already linked to ${channel.id}`)
  }

  await nextStep()

  const newDestination = await client.showDestination.create({
    data: {
      channelId: channel.id,
      channelType: DBChannelType.FORUM,
      show: {
        connectOrCreate: {
          where: {
            imdbId: imdbId,
          },
          create: {
            imdbId: imdbId,
            tvdbId: tvdbSeries.id,
            name: tvdbSeries.name,
          }
        }
      }
    },
    include: {
      show: true,

    }
  })

  await nextStep()

  await updateDBEpisodes(newDestination.show)
  await scheduleAiringMessages(app)

  console.log(`Added show ${tvdbSeries.name} (${imdbId})`)
  return await nextStep(`Linked show \`${tvdbSeries.name}\` to <#${channel.id}>`)
}
