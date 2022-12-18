import { CacheType, ChatInputCommandInteraction, Message, SlashCommandBuilder, SlashCommandSubcommandsOnlyBuilder } from "discord.js"

export type Command<> = {
    data: SlashCommandBuilder | SlashCommandSubcommandsOnlyBuilder
    execute(interaction: ChatInputCommandInteraction<CacheType>): Promise<void | Message<boolean>>
}
