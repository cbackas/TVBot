import { CacheType, ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js'
import { App } from '../app'
import { CommandV2 } from '../interfaces/command'

const slashCommand = new SlashCommandBuilder()
  .setName('ping')
  .setDescription('Replies with Pong!')

const execute = async (app: App, interaction: ChatInputCommandInteraction<CacheType>) => {
  await interaction.reply('Pong!')
}

/**
 * Basic command example, not actually sent to Discord
 */
export const command: CommandV2 = {
  slashCommand: {
    main: slashCommand
  },
  execute
}

