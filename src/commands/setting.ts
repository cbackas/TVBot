import { Channel, ChannelType, ChatInputCommandInteraction, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder, SlashCommandSubcommandGroupBuilder } from 'discord.js'
import { CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { SettingsManager } from '../lib/settingsManager'
import { Destination } from '@prisma/client'

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName('setting')
      .setDescription('Configure various bot settings')
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName('tv_forum')
        .setDescription('Set the forum ID for TV shows')
        .addChannelOption(option => option.setName('channel')
          .setDescription('The channel to set as the TV forum')
          .addChannelTypes(ChannelType.GuildForum)
          .setRequired(true))
    ],
    subGroups: [
      {
        main: new SlashCommandSubcommandGroupBuilder()
          .setName('all_episodes')
          .setDescription('Add or remove a channel from the list that receive all episode notifications'),
        subCommands: [
          new SlashCommandSubcommandBuilder()
            .setName('add')
            .setDescription('Add a channel from the list that receive all episode notifications')
            .addChannelOption(option => option.setName('channel')
              .setDescription('Channel to add to the list that recieves all episode notifications')
              .addChannelTypes(ChannelType.GuildText)
              .setRequired(true)),
          new SlashCommandSubcommandBuilder()
            .setName('remove')
            .setDescription('Remove a channel from the list that receive all episode notifications')
            .addChannelOption(option => option.setName('channel')
              .setDescription('Channel to remove from the list that recieves all episode notifications')
              .addChannelTypes(ChannelType.GuildText)
              .setRequired(true))
        ]
      },
      {
        main: new SlashCommandSubcommandGroupBuilder()
          .setName('morning_summary')
          .setDescription('Settings related to the daily morning summary message'),
        subCommands: [
          new SlashCommandSubcommandBuilder()
            .setName('add_channel')
            .setDescription('Add a channel to the list that recieves the morning summary message')
            .addChannelOption(option => option.setName('channel')
              .setDescription('Channel to add to the list that recieves the morning summary message')
              .addChannelTypes(ChannelType.GuildText)
              .setRequired(true)),
          new SlashCommandSubcommandBuilder()
            .setName('remove_channel')
            .setDescription('Remove a channel from the list that recieves the morning summary message')
            .addChannelOption(option => option.setName('channel')
              .setDescription('Channel to remove from the list that recieves the morning summary message')
              .addChannelTypes(ChannelType.GuildText)
              .setRequired(true))
        ]
      }
    ]
  },
  async execute(app: App, interaction: ChatInputCommandInteraction) {
    const subCommand = interaction.options.getSubcommand()
    const subcommandGroup = interaction.options.getSubcommandGroup()
    const channel = interaction.options.getChannel('channel', true) as Channel

    const settingsManager = app.getSettingsManager()

    /**
     * Handle the TV forum setting
     */
    if (subcommandGroup === null && subCommand === 'tv_forum') {
      return await setTVForum(settingsManager, interaction, channel)
    }

    /**
     * Handle adding channels to the all_episodes list
     */
    if (subcommandGroup === 'all_episodes') {
      return await updateGlobalChannels(settingsManager, interaction, channel, subCommand)
    }

    /**
     * Handle all the morning summary settings
     */
    if (subcommandGroup === 'morning_summary') {
      return await updateMorningSummaryChannels(settingsManager, interaction)
    }
  }
}

/**
 * Sets the default TV forum in the DB
 * @param interaction discord command interaction
 * @param channel channel to set as the TV forum
 */
const setTVForum = async (settingsManager: SettingsManager, interaction: ChatInputCommandInteraction, channel: Channel) => {
  if (channel.type != ChannelType.GuildForum) {
    return await interaction.editReply('Invalid channel type')
  }

  const progressMessage = new ProgressMessageBuilder()
    .addStep(`Setting TV forum to ${channel.name}`)

  await interaction.editReply(progressMessage.nextStep())

  // update the db with the new value
  await settingsManager.update({
    defaultForum: channel.id
  })

  await interaction.editReply(progressMessage.nextStep())
}

/**
 * Update the list of channels that receive all episode notifications
 * @param interaction discord interaction
 * @param channel channel to add/remove from the list
 * @param mode `add` or `remove`
 */
const updateGlobalChannels = async (settingsManager: SettingsManager, interaction: ChatInputCommandInteraction, channel: Channel, mode: string) => {
  // validate the mode
  if (mode !== 'add' && mode !== 'remove') {
    return await interaction.editReply('Invalid mode')
  }

  // validate the channel type
  if (channel.type != ChannelType.GuildText) {
    return await interaction.editReply('Invalid channel type')
  }

  const progress = new ProgressMessageBuilder(interaction)
    .addStep('Updating `All Episodes` channel list')

  await progress.sendNextStep()

  let destinations: Destination[] = []

  // add or remove the channel from the list
  if (mode === 'add') {
    destinations = await settingsManager.addGlobalDestination(channel.id)
  } else if (mode === 'remove') {
    destinations = await settingsManager.removeGlobalDestination(channel.id)
  }

  const destinationsString = destinations.map(d => `<#${d.channelId}>`).join('\n')
  return await progress.sendNextStep(`__Global Destinations__:\nThese channels recieve notifications for all new episodes\n\n${destinationsString}`)
}

/**
 * handle the morning_sumarry commands
 * allows adding and removing channels from the list of channels that receive the morning summary message
 * todo allow setting the time of the morning summary
 * @param settingsManager pass the settingsManager to avoid having to fetch it multiple times
 * @param interaction the chat interaction that got us here
 * @returns nothin
 */
const updateMorningSummaryChannels = async (settingsManager: SettingsManager, interaction: ChatInputCommandInteraction) => {
  const subCommand = interaction.options.getSubcommand() // add_channel or remove_channel


  if (subCommand !== 'add_channel' && subCommand !== 'remove_channel') {
    return await interaction.editReply('Invalid subcommand')
  }

  const channel = interaction.options.getChannel('channel', true) as Channel
  // validate the channel type
  if (channel.type != ChannelType.GuildText) {
    return await interaction.editReply('Invalid channel type')
  }

  const progress = new ProgressMessageBuilder(interaction)
    .addStep('Updating `Morning Summary` channel list')

  await progress.sendNextStep()

  let channelList = settingsManager.fetch()?.morningSummaryDestinations ?? []
  const hasChannel = channelList.some(d => d.channelId === channel.id)

  // add or remove the channel from the list
  if (subCommand === 'add_channel') {
    if (hasChannel) return await interaction.editReply('Channel already in list')
    channelList.push({
      channelId: channel.id,
      forumId: null
    })
  } else if (subCommand === 'remove_channel') {
    if (!hasChannel) return await interaction.editReply('Channel not in morning_summary list')
    channelList = channelList.filter(d => d.channelId !== channel.id)
  }

  await settingsManager.update({
    morningSummaryDestinations: channelList
  })

  return await progress.sendNextStep(`__New List__:\n${channelList.map(d => `<#${d.channelId}>`).join('\n')}`)
}
