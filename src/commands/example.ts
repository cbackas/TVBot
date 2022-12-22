import { CacheType, ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js'
import { App } from '../app'
import { Command } from '../interfaces/command'

/**
 * Basic command example, not actually sent to Discord
 */
const command: Command = {
  data: new SlashCommandBuilder()
    .setName('ping')
    .setDescription('Replies with Pong!'),
  async execute(_app: App, interaction: ChatInputCommandInteraction<CacheType>) {
    await interaction.reply('Pong!')
  }
}

export default command 
