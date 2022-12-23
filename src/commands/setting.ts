import { APIInteractionDataResolvedChannel, CacheType, ChannelType, ChatInputCommandInteraction, ForumChannel, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder, SlashCommandSubcommandGroupBuilder } from 'discord.js'
import client from '../lib/prisma'
import { Command } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'

/**
 * The `/setting` command definition
 */
const slashCommand = new SlashCommandBuilder()
  .setName('setting')
  .setDescription('Configure various bot settings')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
  .addSubcommand(new SlashCommandSubcommandBuilder()
    .setName('tv_forum')
    .setDescription('Set the forum ID for TV shows')
    .addChannelOption(option => option.setName('channel')
      .setDescription('The channel to set as the TV forum')
      .addChannelTypes(ChannelType.GuildForum)
      .setRequired(true)))
  .addSubcommandGroup(new SlashCommandSubcommandGroupBuilder()
    .setName('all_episodes')
    .setDescription('Add or remove a channel from the list that receive all episode notifications')
    .addSubcommand(new SlashCommandSubcommandBuilder()
      .setName('add')
      .setDescription('Add a channel from the list that receive all episode notifications')
      .addChannelOption(option => option.setName('channel')
        .setDescription('Channel to add to the list that recieves all episode notifications')
        .addChannelTypes(ChannelType.GuildText)
        .setRequired(true)))
    .addSubcommand(new SlashCommandSubcommandBuilder()
      .setName('remove')
      .setDescription('Remove a channel from the list that receive all episode notifications')
      .addChannelOption(option => option.setName('channel')
        .setDescription('Channel to remove from the list that recieves all episode notifications')
        .addChannelTypes(ChannelType.GuildText)
        .setRequired(true))))

/**
 * The main execution method for the `/setting` command
 * @param app main application object instance
 * @param interaction the discord interaction that triggered the command
 * @returns nothing important
 */
const execute = async (app: App, interaction: ChatInputCommandInteraction) => {
  const subCommand = interaction.options.getSubcommand()
  const subcommandGroup = interaction.options.getSubcommandGroup()

  /**
   * Handle the TV forum setting
   */
  if (subcommandGroup === null && subCommand === 'tv_forum') {
    const channel = interaction.options.getChannel('channel', true)

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

  }

  /**
   * Handle removing channels from the all_episodes list
   */
  if (subcommandGroup === 'all_episodes' && subCommand === 'remove') {

  }

  return await interaction.editReply('Invalid subcommand')
}

export const command: Command = {
  data: slashCommand,
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
