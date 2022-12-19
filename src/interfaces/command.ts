import { CacheType, ChatInputCommandInteraction, InteractionResponse, Message, SlashCommandBuilder, SlashCommandSubcommandsOnlyBuilder } from "discord.js"
import { App } from "../app"

type ExecuteFunction = Promise<void | Message<boolean> | InteractionResponse<boolean>>

export type Command<> = {
  data: SlashCommandBuilder | SlashCommandSubcommandsOnlyBuilder
  execute(app: App, interaction: ChatInputCommandInteraction<CacheType>): ExecuteFunction
}
