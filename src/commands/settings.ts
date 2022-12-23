import { APIInteractionDataResolvedChannel, CacheType, ChannelType, ChatInputCommandInteraction, ForumChannel, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder, SlashCommandSubcommandGroupBuilder } from 'discord.js'
import client from '../lib/prisma'
import { Command } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'

const slashCommand = new SlashCommandBuilder()
  .setName('setting')
  .setDescription('Configura various bot settings')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
  .addSubcommand(new SlashCommandSubcommandBuilder()
    .setName('tv_forum')
    .setDescription('Set the forum ID for TV shows')
    .addChannelOption(option => option.setName('channel')
      .setDescription('The channel to set as the TV forum')
      .addChannelTypes(ChannelType.GuildForum)))
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

const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  const subCommand = interaction.options.getSubcommand()

  if (subCommand === 'tv_forum') {
    const channel = interaction.options.getChannel('channel', true)

    if (channel.type != ChannelType.GuildForum) {
      return await interaction.editReply('Invalid channel type')
    }

    await setTVForum(interaction, channel)
    await app.loadSettings()
  }

  return await interaction.editReply('Invalid subcommand')
}

export const command: Command = {
  data: slashCommand,
  execute
}

const setTVForum = async (interaction: ChatInputCommandInteraction<CacheType>, channel: ForumChannel | APIInteractionDataResolvedChannel) => {
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

  await interaction.editReply(progressMessage.nextStep() + "\n\n Some kinda warning about if theres an old forum with posts assigned")
}
