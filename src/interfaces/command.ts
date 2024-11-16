import {
  type AnySelectMenuInteraction,
  type AutocompleteInteraction,
  type ChatInputCommandInteraction,
  type InteractionResponse,
  type Message,
  type SlashCommandBuilder,
  type SlashCommandOptionsOnlyBuilder,
  type SlashCommandSubcommandBuilder,
  type SlashCommandSubcommandGroupBuilder,
  type SlashCommandSubcommandsOnlyBuilder,
} from "npm:discord.js"
import { type App } from "app.ts"

type ExecuteFunction = void | Message<boolean> | InteractionResponse<boolean>

export interface CommandV2 {
  slashCommand: {
    main:
      | SlashCommandBuilder
      | Omit<SlashCommandBuilder, "addSubcommand" | "addSubcommandGroup">
      | SlashCommandSubcommandsOnlyBuilder
      | SlashCommandOptionsOnlyBuilder
    subCommands?: SlashCommandSubcommandBuilder[]
    subGroups?: Array<{
      main: SlashCommandSubcommandGroupBuilder
      subCommands: SlashCommandSubcommandBuilder[]
    }>
  }
  selectMenuIds?: string[]
  executeCommand: (
    app: App,
    interaction: ChatInputCommandInteraction,
  ) => Promise<ExecuteFunction>
  executeAutoComplate?: (
    app: App,
    interaction: AutocompleteInteraction,
  ) => Promise<ExecuteFunction>
  executeSelectMenu?: (
    app: App,
    interaction: AnySelectMenuInteraction,
  ) => Promise<ExecuteFunction>
}
