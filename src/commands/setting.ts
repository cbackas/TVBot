import { APIInteractionDataResolvedChannel, ChannelType, ChatInputCommandInteraction, ForumChannel, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder, SlashCommandSubcommandGroupBuilder } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { Settings } from '@prisma/client'

/**
 * The main execution method for the `/setting` command
 * @param app main application object instance
 * @param interaction the discord interaction that triggered the command
 * @returns nothing important
 */
const execute = async (app: App, interaction: ChatInputCommandInteraction) => {
  const subCommand = interaction.options.getSubcommand()
  const subcommandGroup = interaction.options.getSubcommandGroup()
  const channel = interaction.options.getChannel('channel', true)

  /**
   * Handle the TV forum setting
   */
  if (subcommandGroup === null && subCommand === 'tv_forum') {
    if (channel.type != ChannelType.GuildForum) {
      return await interaction.editReply('Invalid channel type')
    }

    await setTVForum(interaction, channel)
    return await app.loadSettings()
  }

  /**
   * Handle adding channels to the all_episodes list
   */
  if (subcommandGroup === 'all_episodes' && subCommand === 'add') {
    if (channel.type != ChannelType.GuildText) {
      return await interaction.editReply('Invalid channel type')
    }

    const currentChannelList = await getCurrentGlobalChannels()

    if (currentChannelList.includes(channel.id)) {
      return await interaction.editReply('Channel already in list')
    }

    currentChannelList.push(channel.id)

    await updateGlobalChannels(currentChannelList)

    return await interaction.editReply(`${currentChannelList.map(id => `<#${id}>`).join('\n')}`)
  }

  /**
   * Handle removing channels from the all_episodes list
   */
  if (subcommandGroup === 'all_episodes' && subCommand === 'remove') {
    if (channel.type != ChannelType.GuildText) {
      return await interaction.editReply('Invalid channel type')
    }

    const currentChannelList = await getCurrentGlobalChannels()

    if (!currentChannelList.includes(channel.id)) {
      return await interaction.editReply('Channel not in list')
    }

    const index = currentChannelList.indexOf(channel.id)
    currentChannelList.splice(index, 1)

    await updateGlobalChannels(currentChannelList)

    return await interaction.editReply(`${currentChannelList.map(id => `<#${id}>`).join('\n')}`)
  }
}

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
  execute
}

/**
 * Sets the default TV forum in the DB
 * @param interaction discord command interaction
 * @param channel channel to set as the TV forum
 */
const setTVForum = async (interaction: ChatInputCommandInteraction, channel: ForumChannel | APIInteractionDataResolvedChannel) => {
  const progressMessage = new ProgressMessageBuilder()
    .addStep(`Setting TV forum to ${channel.name}`)

  await interaction.editReply(progressMessage.nextStep())

  await client.settings.upsert({
    where: {
      key: 'tv_forum',
    },
    update: {
      value: channel.id,
    },
    create: {
      key: 'tv_forum',
      value: channel.id,
    }
  })

  await interaction.editReply(progressMessage.nextStep())
}

const getCurrentGlobalChannels = async () => {
  const currentChannels: Settings = await client.settings.findUnique({
    where: {
      key: 'all_episodes'
    }
  }) ?? { key: 'all_episodes', value: JSON.stringify([]) }
  return JSON.parse(currentChannels.value) as string[]
}

const updateGlobalChannels = async (newList: string[]) => {
  await client.settings.upsert({
    where: {
      key: 'all_episodes'
    },
    update: {
      value: JSON.stringify(newList)
    },
    create: {
      key: 'all_episodes',
      value: JSON.stringify(newList)
    }
  })
}
