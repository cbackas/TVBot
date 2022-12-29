import { Channel, ChannelType, ChatInputCommandInteraction, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder, SlashCommandSubcommandGroupBuilder, TextBasedChannel } from 'discord.js'
import { CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { Destination } from '@prisma/client'
import { SettingsManager } from '../lib/settingsManager'

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
      await setTVForum(settingsManager, interaction, channel)
      return
    }

    /**
     * Handle adding channels to the all_episodes list
     */
    if (subcommandGroup === 'all_episodes') {
      return await updateGlobalChannels(settingsManager, interaction, channel, subCommand)
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

  const progress = new ProgressMessageBuilder()
    .addStep('Updating `All Episodes` channel list')

  await interaction.editReply(progress.nextStep())

  const destination: Destination = {
    channelId: channel.id,
    forumId: null
  }

  const channelList = settingsManager.fetch()?.allEpisodes ?? []
  // index of the channel in the existing list
  const existingIndex = channelList.indexOf(destination)

  // add or remove the channel from the list
  if (mode === 'add') {
    if (existingIndex !== -1) return await interaction.editReply('Channel already in list')
    channelList.push(destination)
  } else if (mode === 'remove') {
    if (existingIndex == -1) return await interaction.editReply('Channel not in all_episodes list')
    channelList.splice(existingIndex, 1)
  }

  await settingsManager.update({
    allEpisodes: channelList
  })

  return await interaction.editReply(progress.nextStep() + `\n\n__New List__:\n${channelList.map(id => `<#${id}>`).join('\n')}`)
}
