import { APIInteractionDataResolvedChannel, CacheType, ChannelType, ChatInputCommandInteraction, ForumChannel, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder } from 'discord.js'
import client from '../lib/prisma'
import { Command } from '../interfaces/command'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'

const slashCommand = new SlashCommandBuilder()
  .setName('settings')
  .setDescription('Configura various bot settings')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
  .addSubcommand(new SlashCommandSubcommandBuilder()
    .setName('tv_forum')
    .setDescription('Set the forum ID for TV shows')
    .addChannelOption(option => option.setName('channel')
      .setDescription('The channel to set as the TV forum')
      .addChannelTypes(ChannelType.GuildForum)
    )
  )

const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  const subCommand = interaction.options.getSubcommand()
  if (!subCommand) return await interaction.editReply('Invalid subcommand')

  if (subCommand === 'tv_forum') {
    const channel = interaction.options.getChannel('channel', true)

    if (channel.type != ChannelType.GuildForum) {
      return await interaction.editReply('Invalid channel type')
    }

    await setTVForum(interaction, channel)
  }

  return await app.loadSettings()
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
