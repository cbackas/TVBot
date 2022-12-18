import { CacheType, ChatInputCommandInteraction, SlashCommandBuilder } from 'discord.js'
import { Command } from '../interfaces/command'

const command: Command = {
  data: new SlashCommandBuilder()
    .setName('ping')
    .setDescription('Replies with Pong!'),
  async execute(interaction: ChatInputCommandInteraction<CacheType>) {
    await interaction.reply('Pong!')
  }
}

export default command 
