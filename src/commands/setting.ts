import {
  ActionRowBuilder,
  type AnySelectMenuInteraction,
  type Channel,
  ChannelType,
  type ChatInputCommandInteraction,
  InteractionContextType,
  type Message,
  PermissionFlagsBits,
  SlashCommandBuilder,
  SlashCommandSubcommandBuilder,
  SlashCommandSubcommandGroupBuilder,
  StringSelectMenuBuilder,
} from "discord.js"
import { type CommandV2 } from "interfaces/command.ts"
import { ProgressMessageBuilder } from "lib/progressMessages.ts"
import { LOG_TYPES } from "lib/discordLog.ts"
import { Settings } from "lib/settingsManager.ts"
import { type Destination } from "prisma-client/client.ts"

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("setting")
      .setDescription("Configure various bot settings")
      .setContexts(InteractionContextType.Guild)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName("tv_forum")
        .setDescription("Set the forum ID for TV shows")
        .addChannelOption((option) =>
          option.setName("channel")
            .setDescription("The channel to set as the TV forum")
            .addChannelTypes(ChannelType.GuildForum)
            .setRequired(true)
        ),
      new SlashCommandSubcommandBuilder()
        .setName("log_channel")
        .setDescription("Set the channel that the bot posts log messages to")
        .addChannelOption((option) =>
          option.setName("channel")
            .setDescription("The channel to post logs in")
            .addChannelTypes(ChannelType.GuildText)
            .setRequired(true)
        ),
      new SlashCommandSubcommandBuilder()
        .setName("log_types")
        .setDescription("Choose which types of log message get posted"),
    ],
    subGroups: [
      {
        main: new SlashCommandSubcommandGroupBuilder()
          .setName("all_episodes")
          .setDescription(
            "Add or remove a channel from the list that receive all episode notifications",
          ),
        subCommands: [
          new SlashCommandSubcommandBuilder()
            .setName("add")
            .setDescription(
              "Add a channel to the list that receives all episode notifications",
            )
            .addChannelOption((option) =>
              option.setName("channel")
                .setDescription(
                  "Channel to add to the list that receives all episode notifications",
                )
                .addChannelTypes(
                  ChannelType.GuildText,
                  ChannelType.GuildAnnouncement,
                )
                .setRequired(true)
            ),
          new SlashCommandSubcommandBuilder()
            .setName("remove")
            .setDescription(
              "Remove a channel from the list that receives all episode notifications",
            )
            .addChannelOption((option) =>
              option.setName("channel")
                .setDescription(
                  "Channel to remove from the list that receives all episode notifications",
                )
                .addChannelTypes(
                  ChannelType.GuildText,
                  ChannelType.GuildAnnouncement,
                )
                .setRequired(true)
            ),
        ],
      },
      {
        main: new SlashCommandSubcommandGroupBuilder()
          .setName("morning_summary")
          .setDescription(
            "Settings related to the daily morning summary message",
          ),
        subCommands: [
          new SlashCommandSubcommandBuilder()
            .setName("add_channel")
            .setDescription(
              "Add a channel to the list that receives the morning summary message",
            )
            .addChannelOption((option) =>
              option.setName("channel")
                .setDescription(
                  "Channel to add to the list that receives the morning summary message",
                )
                .addChannelTypes(
                  ChannelType.GuildText,
                  ChannelType.GuildAnnouncement,
                )
                .setRequired(true)
            ),
          new SlashCommandSubcommandBuilder()
            .setName("remove_channel")
            .setDescription(
              "Remove a channel from the list that receives the morning summary message",
            )
            .addChannelOption((option) =>
              option.setName("channel")
                .setDescription(
                  "Channel to remove from the list that receives the morning summary message",
                )
                .addChannelTypes(
                  ChannelType.GuildText,
                  ChannelType.GuildAnnouncement,
                )
                .setRequired(true)
            ),
        ],
      },
    ],
  },
  async executeCommand(interaction: ChatInputCommandInteraction) {
    const subCommand = interaction.options.getSubcommand()
    const subcommandGroup = interaction.options.getSubcommandGroup()

    /**
     * Handle the log types setting, this subcommand has no channel option
     */
    if (subcommandGroup === null && subCommand === "log_types") {
      return await interaction.editReply(
        buildLogTypesReply(Settings.fetch()?.disabledLogTypes ?? []),
      )
    }

    const channel = interaction.options.getChannel("channel", true) as Channel

    /**
     * Handle the TV forum setting
     */
    if (subcommandGroup === null && subCommand === "tv_forum") {
      await setTVForum(interaction, channel)
      return
    }

    /**
     * Handle the log channel setting
     */
    if (subcommandGroup === null && subCommand === "log_channel") {
      return await setLogChannel(interaction, channel)
    }

    /**
     * Handle adding channels to the all_episodes list
     */
    if (subcommandGroup === "all_episodes") {
      return await updateGlobalChannels(
        interaction,
        channel,
        subCommand,
      )
    }

    /**
     * Handle all the morning summary settings
     */
    if (subcommandGroup === "morning_summary") {
      return await updateMorningSummaryChannels(interaction)
    }
  },
  selectMenuIds: ["log_types_menu"],
  async executeSelectMenu(interaction: AnySelectMenuInteraction) {
    const disabledLogTypes = LOG_TYPES.map((type) => type.key)
      .filter((key) => !interaction.values.includes(key))

    await Settings.update({
      disabledLogTypes,
    })

    return await interaction.editReply(buildLogTypesReply(disabledLogTypes))
  },
}

/**
 * Builds the reply containing the multi-select menu of log types
 * @param disabled keys of the log types that are currently turned off
 */
function buildLogTypesReply(disabled: string[]) {
  const menu = new StringSelectMenuBuilder()
    .setCustomId("log_types_menu")
    .setPlaceholder("Nothing selected")
    .setMinValues(0)
    .setMaxValues(LOG_TYPES.length)
    .addOptions(LOG_TYPES.map((type) => ({
      label: type.label,
      description: type.description,
      value: type.key,
      default: !disabled.includes(type.key),
    })))

  const noChannelNote = Settings.fetch()?.logChannel == null
    ? "\nNo log channel is set yet, use `/setting log_channel`."
    : ""

  return {
    content: `Select the types of message to post in the log channel.${noChannelNote}`,
    components: [
      new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(menu),
    ],
  }
}

/**
 * Sets the default TV forum in the DB
 * @param interaction discord command interaction
 * @param channel channel to set as the TV forum
 */
async function setTVForum(
  interaction: ChatInputCommandInteraction,
  channel: Channel,
): Promise<Message<boolean> | void> {
  if (channel.type !== ChannelType.GuildForum) {
    return await interaction.editReply("Invalid channel type")
  }

  const progressMessage = new ProgressMessageBuilder()
    .addStep(`Setting TV forum to ${channel.name}`)

  await interaction.editReply(progressMessage.nextStep())

  // update the db with the new value
  await Settings.update({
    defaultForum: channel.id,
  })

  await interaction.editReply(progressMessage.nextStep())
}

/**
 * Sets the channel that the bot posts log messages to
 * @param interaction discord command interaction
 * @param channel channel to set as the log channel
 */
async function setLogChannel(
  interaction: ChatInputCommandInteraction,
  channel: Channel,
): Promise<Message<boolean> | void> {
  if (channel.type !== ChannelType.GuildText) {
    return await interaction.editReply("Invalid channel type")
  }

  try {
    await channel.send("The bot will post log messages in this channel.")
  } catch (error) {
    console.warn(`Could not send to proposed log channel ${channel.id}:`, error)
    return await interaction.editReply(
      `I can't send messages in <#${channel.id}>. Check my permissions there and try again.`,
    )
  }

  await Settings.update({
    logChannel: channel.id,
  })

  return await interaction.editReply(
    `Log messages will be posted in <#${channel.id}>`,
  )
}

/**
 * Update the list of channels that receive all episode notifications
 * @param interaction discord interaction
 * @param channel channel to add/remove from the list
 * @param mode `add` or `remove`
 */
async function updateGlobalChannels(
  interaction: ChatInputCommandInteraction,
  channel: Channel,
  mode: string,
): Promise<Message<boolean>> {
  // validate the mode
  if (mode !== "add" && mode !== "remove") {
    return await interaction.editReply("Invalid mode")
  }

  // validate the channel type
  if (
    ![ChannelType.GuildText, ChannelType.GuildAnnouncement].includes(
      channel.type,
    )
  ) {
    return await interaction.editReply("Invalid channel type")
  }

  const progress = new ProgressMessageBuilder(interaction)
    .addStep("Updating `All Episodes` channel list")

  await progress.sendNextStep()

  let destinations: Destination[] = []

  // add or remove the channel from the list
  if (mode === "add") {
    destinations = await Settings.addGlobalDestination(channel.id)
  } else if (mode === "remove") {
    destinations = await Settings.removeGlobalDestination(channel.id)
  }

  const destinationsString = destinations.map((d) => `<#${d.channelId}>`).join(
    "\n",
  )
  return await progress.sendNextStep(`__New List__:\nn${destinationsString}`)
}

/**
 * handle the morning_sumarry commands
 * allows adding and removing channels from the list of channels that receive the morning summary message
 * todo allow setting the time of the morning summary
 * @param settingsManager pass the settingsManager to avoid having to fetch it multiple times
 * @param interaction the chat interaction that got us here
 * @returns nothin
 */
async function updateMorningSummaryChannels(
  interaction: ChatInputCommandInteraction,
): Promise<Message<boolean>> {
  const subCommand = interaction.options.getSubcommand() // add_channel or remove_channel

  if (subCommand !== "add_channel" && subCommand !== "remove_channel") {
    return await interaction.editReply("Invalid subcommand")
  }

  const channel = interaction.options.getChannel("channel", true) as Channel
  // validate the channel type
  if (
    ![ChannelType.GuildText, ChannelType.GuildAnnouncement].includes(
      channel.type,
    )
  ) {
    return await interaction.editReply("Invalid channel type")
  }

  const progress = new ProgressMessageBuilder(interaction)
    .addStep("Updating `Morning Summary` channel list")

  await progress.sendNextStep()

  let channelList = Settings.fetch()?.morningSummaryDestinations ?? []
  const hasChannel = channelList.some((d) => d.channelId === channel.id)

  // add or remove the channel from the list
  if (subCommand === "add_channel") {
    if (hasChannel) {
      return await interaction.editReply("Channel already in list")
    }
    channelList.push({
      channelId: channel.id,
      forumId: null,
    })
  } else if (subCommand === "remove_channel") {
    if (!hasChannel) {
      return await interaction.editReply("Channel not in morning_summary list")
    }
    channelList = channelList.filter((d) => d.channelId !== channel.id)
  }

  await Settings.update({
    morningSummaryDestinations: channelList,
  })

  const channelsString = channelList.map((d) => `<#${d.channelId}>`).join("\n")
  return await progress.sendNextStep(`__New List__:\n${channelsString}`)
}
