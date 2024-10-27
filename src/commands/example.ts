import {
  type CacheType,
  type ChatInputCommandInteraction,
  SlashCommandBuilder,
} from "discord.js"
import { type App } from "app.ts"
import { type CommandV2 } from "interfaces/command.ts"

const slashCommand = new SlashCommandBuilder()
  .setName("ping")
  .setDescription("Replies with Pong!")

const executeCommand = async (
  app: App,
  interaction: ChatInputCommandInteraction<CacheType>,
): Promise<void> => {
  await interaction.reply("Pong!")
}

/**
 * Basic command example, not actually sent to Discord
 */
export const command: CommandV2 = {
  slashCommand: {
    main: slashCommand,
  },
  executeCommand,
}
