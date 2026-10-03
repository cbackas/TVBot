import {
  ActionRowBuilder,
  type AnySelectMenuInteraction,
  ChannelType,
  type ChatInputCommandInteraction,
  InteractionContextType,
  PermissionFlagsBits,
  SlashCommandBuilder,
  SlashCommandSubcommandBuilder,
  StringSelectMenuBuilder,
  type TextBasedChannel,
} from "discord.js"
import client from "lib/prisma.ts"
import { type CommandV2 } from "interfaces/command.ts"
import { ProgressError } from "interfaces/error.ts"
import { ProgressMessageBuilder } from "lib/progressMessages.ts"
import { pruneUnsubscribedShows } from "lib/shows.ts"
import {
  codeSpan,
  type CommandLog,
  logCommandUse,
  rememberCommandLog,
  takeCommandLog,
} from "lib/discordLog.ts"

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("unlink")
      .setDescription("Unlink shows from a channel for notifications.")
      .setContexts(InteractionContextType.Guild)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageThreads),
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
  async executeCommand(interaction: ChatInputCommandInteraction) {
    const log = logCommandUse(interaction)
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
      void log.fail("invalid channel")
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
        void log.append("(no shows linked)")
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

      const reply = await interaction.editReply({
        content:
          `Select the shows that you'd like to unlink from <#${channel?.id}>`,
        components: [row],
      })
      rememberCommandLog(reply.id, log)
    } catch (error) {
      // catch our custom error and display it for the user
      if (error instanceof ProgressError) {
        void log.fail(error.message)
        const message = `${progress.toString()}\n\nError: ${error.message}`
        return await interaction.editReply(message)
      }

      void log.fail()
      throw error
    }
  },
  async executeSelectMenu(interaction: AnySelectMenuInteraction) {
    const log = takeCommandLog(interaction.message.id)
    const channelId = interaction.message.content.match(/<#([0-9]+)>/)?.at(1)

    if (channelId === undefined) {
      void log?.fail("could not find the channel")
      return await interaction.reply("Failed to find channel")
    }

    try {
      return await unlinkShows(interaction, channelId, log)
    } catch (error) {
      void log?.fail()
      throw error
    }
  },
}

/**
 * Unlinks the shows chosen in the select menu from a channel
 * @param interaction select menu interaction with the chosen shows
 * @param channelId channel to unlink the shows from
 * @param log log handle for the original /unlink command, if it's still remembered
 */
async function unlinkShows(
  interaction: AnySelectMenuInteraction,
  channelId: string,
  log: CommandLog | undefined,
) {
  const values = interaction.values

  const shows = await client.show.findMany({
    where: { imdbId: { in: values } },
    select: { name: true, imdbId: true },
  })

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

  void log?.success(
    `Unlinked ${
      shows.map((show) => `${codeSpan(show.name)} (${show.imdbId})`).join(", ")
    } from <#${channelId}>`,
  )

  return await interaction.editReply({
    content: `Unlinked ${s.count} shows from <#${channelId}>`,
    components: [],
  })
}
