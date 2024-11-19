import {
  ActionRowBuilder,
  ChannelType,
  type ChatInputCommandInteraction,
  PermissionFlagsBits,
  type SelectMenuInteraction,
  SlashCommandBuilder,
  SlashCommandSubcommandBuilder,
  StringSelectMenuBuilder,
  type TextBasedChannel,
} from "npm:discord.js"
import client from "lib/prisma.ts"
import { type CommandV2 } from "interfaces/command.ts"
import { type App } from "app.ts"
import { ProgressError } from "interfaces/error.ts"
import { ProgressMessageBuilder } from "lib/progressMessages.ts"
import { pruneUnsubscribedShows } from "lib/shows.ts"

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("unlink")
      .setDescription("Unlink shows from a channel for notifications.")
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName("here")
        .setDescription(
          "Unlink shows from the current channel for notifications.",
        ),
      new SlashCommandSubcommandBuilder()
        .setName("channel")
        .setDescription("Unlink shows from a channel for notifications.")
        .addChannelOption((option) =>
          option.setName("channel")
            .setDescription("The channel to unlink from announcements")
            .addChannelTypes(ChannelType.GuildText)
            .setRequired(true)
        ),
    ],
  },
  selectMenuIds: ["unlink_shows_menu"],
  async executeCommand(_app: App, interaction: ChatInputCommandInteraction) {
    const subCommand = interaction.options.getSubcommand()

    const progress = new ProgressMessageBuilder()
      .addStep("Check for existing show subscription")
      .addStep("Fetching upcoming episodes")

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

    // error if the channel didnt get set for some reason
    if (channel === undefined) {
      return await interaction.editReply("Invalid channel")
    }

    try {
      const showsInChannel = await client.show.findMany({
        where: {
          destinations: {
            some: {
              channelId: channel?.id,
            },
          },
        },
      })

      if (showsInChannel === null || showsInChannel.length === 0) {
        return await interaction.editReply(
          "This channel has no shows linked to it.",
        )
      }

      const row = new ActionRowBuilder<StringSelectMenuBuilder>()
        .addComponents(
          new StringSelectMenuBuilder()
            .setCustomId("unlink_shows_menu")
            .setPlaceholder("Nothing selected")
            .setMinValues(1)
            .setMaxValues(showsInChannel.length)
            .addOptions(showsInChannel.map((s) => {
              return {
                label: s.name,
                value: s.imdbId,
              }
            })),
        )

      await interaction.editReply({
        content:
          `Select the shows that you'd like to unlink from <#${channel?.id}>`,
        components: [row],
      })
    } catch (error) {
      // catch our custom error and display it for the user
      if (error instanceof ProgressError) {
        const message = `${progress.toString()}\n\nError: ${error.message}`
        return await interaction.editReply(message)
      }

      throw error
    }
  },
  async executeSelectMenu(_app, interaction: SelectMenuInteraction) {
    const channelId = interaction.message.content.match(/<#([0-9]+)>/)?.at(1)

    if (channelId === undefined) {
      return await interaction.reply("Failed to find channel")
    }

    const values = interaction.values

    const s = await client.show.updateMany({
      where: {
        imdbId: {
          in: values,
        },
      },
      data: {
        destinations: {
          deleteMany: {
            where: {
              channelId,
            },
          },
        },
      },
    })

    await pruneUnsubscribedShows()

    return await interaction.editReply({
      content: `Unlinked ${s.count} shows from <#${channelId}>`,
      components: [],
    })
  },
}
