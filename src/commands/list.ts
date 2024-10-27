import {
  ChannelType,
  type ChatInputCommandInteraction,
  PermissionFlagsBits,
  SlashCommandBuilder,
  SlashCommandSubcommandBuilder,
  SlashCommandSubcommandGroupBuilder,
  type TextBasedChannel,
} from "discord.js"
import client from "lib/prisma.ts"
import { type CommandV2 } from "interfaces/command.ts"
import { type App } from "app.ts"
import { type Show } from "@prisma/client"

const subCommands = {
  ALL: "all",
  HERE: "here",
  CHANNEL: "channel",
} as const

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("list")
      .setDescription("List various things from the bot's DB")
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels),
    subGroups: [{
      main: new SlashCommandSubcommandGroupBuilder()
        .setName("shows")
        .setDescription("List shows from the bot's DB"),
      subCommands: [
        new SlashCommandSubcommandBuilder()
          .setName(subCommands.HERE)
          .setDescription("List shows linked to the current channel"),
        new SlashCommandSubcommandBuilder()
          .setName(subCommands.CHANNEL)
          .setDescription("List shows linked to a specific channel")
          .addChannelOption((option) =>
            option.setName("channel")
              .setDescription("Channel to list shows from")
              .addChannelTypes(ChannelType.GuildText)
              .setRequired(true)
          ),
      ],
    }],
  },
  async executeCommand(app: App, interaction: ChatInputCommandInteraction) {
    if (interaction.options.getSubcommandGroup() !== "shows") {
      return await interaction.editReply("Invalid subcommand group")
    }

    const subCommand = interaction.options
      .getSubcommand() as typeof subCommands[keyof typeof subCommands]

    let channel: TextBasedChannel | undefined

    // the `here` subcommand links the show to the current channel
    if (subCommand === "here" && interaction.channel !== null) {
      channel = interaction.channel
    }

    // the `channel` subcommand allows a user to specify a text channel
    if (subCommand === "channel") {
      channel = interaction.options.getChannel(
        "channel",
        true,
      ) as TextBasedChannel
    }

    if (channel === undefined) {
      return await interaction.editReply("Invalid channel")
    }

    const showsInChannel = await getShowsInChannel(channel)

    const showMessages = showsInChannel.map((show) => {
      const destinations = show.destinations.map((destination) =>
        `<#${destination.channelId}>`
      ).join(" ")
      return `**${show.name}** ${destinations}`
    }).join("\n")

    await interaction.editReply(
      `Shows in channel <#${channel.id}>:\n\n${showMessages}`,
    )
  },
}

async function getShowsInChannel(channel?: TextBasedChannel): Promise<Show[]> {
  if (channel === undefined) {
    return await client.show.findMany()
  }

  return await client.show.findMany({
    where: {
      destinations: {
        some: {
          channelId: channel.id,
        },
      },
    },
  })
}
