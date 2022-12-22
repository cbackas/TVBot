import { CacheType, ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js'
import { App } from '../app'
import { Command } from '../interfaces/command'

const slashCommand = new SlashCommandBuilder()
  .setName('ping')
  .setDescription('Replies with Pong!')

const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  await interaction.reply('Pong!')
}

/**
 * Basic command example, not actually sent to Discord
 */
export const command: Command = {
  data: slashCommand,
  execute
}

