import { ChatInputCommandInteraction, InteractionResponse, Message, SlashCommandBuilder, SlashCommandSubcommandBuilder, SlashCommandSubcommandGroupBuilder, SlashCommandSubcommandsOnlyBuilder } from "discord.js"
import { App } from "../app"

type ExecuteFunction = Promise<void | Message<boolean> | InteractionResponse<boolean>>

export type CommandV2 = {
  slashCommand: {
    main: SlashCommandBuilder | Omit<SlashCommandBuilder, "addSubcommand" | "addSubcommandGroup"> | SlashCommandSubcommandsOnlyBuilder
    subCommands?: SlashCommandSubcommandBuilder[]
    subGroups?: {
      main: SlashCommandSubcommandGroupBuilder
      subCommands: SlashCommandSubcommandBuilder[]
    }[]
  }
  execute(app: App, interaction: ChatInputCommandInteraction): ExecuteFunction
}
