import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  ComponentType,
  InteractionContextType,
  MessageFlags,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import {
  editInteractionResponse,
  textDisplayComponents,
} from "../lib/discord.js";
import {
  getGuildId,
  getResolvedChannel,
  getSubcommand,
  getSubcommandGroup,
} from "../lib/interactionOptions.js";
import { ProgressMessageBuilder, StepStatus } from "../lib/progressMessages.js";
import {
  addGlobalDestination,
  getDefaultForum,
  getGlobalDestinations,
  removeGlobalDestination,
  setDefaultForum,
} from "../lib/settingsManager.js";
import type { Command } from "./index.js";

const TEXT_CHANNEL_TYPES = new Set<number>([
  ChannelType.GuildText,
  ChannelType.GuildAnnouncement,
]);

function renderList(channelIds: string[]): string {
  if (channelIds.length === 0) return "(none)";
  return channelIds.map((id) => `<#${id}>`).join("\n");
}

export default class SettingCommand implements Command {
  public readonly name = "setting";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "setting" as const,
      description: "Configure bot-wide channels and defaults",
      default_member_permissions: "16",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "view",
          description: "View current settings",
        },
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "default_forum",
          description: "Where /post creates new show threads by default",
          options: [
            {
              type: ApplicationCommandOptionType.Channel,
              name: "channel",
              description: "Forum channel",
              required: true,
              channel_types: [ChannelType.GuildForum],
            },
          ],
        },
        {
          type: ApplicationCommandOptionType.SubcommandGroup,
          name: "global_episode_broadcast",
          description:
            "Channels that receive a broadcast for every airing episode across all tracked shows",
          options: [
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "add",
              description: "Start broadcasting to this channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Text or announcement channel",
                  required: true,
                  channel_types: [
                    ChannelType.GuildText,
                    ChannelType.GuildAnnouncement,
                  ],
                },
              ],
            },
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "remove",
              description: "Stop broadcasting to this channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Text or announcement channel",
                  required: true,
                  channel_types: [
                    ChannelType.GuildText,
                    ChannelType.GuildAnnouncement,
                  ],
                },
              ],
            },
          ],
        },
        {
          type: ApplicationCommandOptionType.SubcommandGroup,
          name: "morning_summary",
          description:
            "Channels that receive the daily digest of upcoming episodes across all tracked shows",
          options: [
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "add",
              description: "Start sending the daily digest to this channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Text or announcement channel",
                  required: true,
                  channel_types: [
                    ChannelType.GuildText,
                    ChannelType.GuildAnnouncement,
                  ],
                },
              ],
            },
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "remove",
              description: "Stop sending the daily digest to this channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Text or announcement channel",
                  required: true,
                  channel_types: [
                    ChannelType.GuildText,
                    ChannelType.GuildAnnouncement,
                  ],
                },
              ],
            },
          ],
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const token = interaction.token;
    const group = getSubcommandGroup(interaction);
    const sub = getSubcommand(interaction);

    const guildId = getGuildId(interaction);
    if (guildId == null) {
      await editInteractionResponse(token, {
        content: "This command can only be used in a server.",
      });
      return;
    }

    // /setting view
    if (group == null && sub === "view") {
      const [forumId, broadcast, digest] = await Promise.all([
        getDefaultForum(guildId),
        getGlobalDestinations("global_episode_broadcast", guildId),
        getGlobalDestinations("morning_summary", guildId),
      ]);

      const body = [
        "__Default Forum__",
        forumId != null ? `<#${forumId}>` : "(none)",
        "",
        "__Global Episode Broadcast__",
        renderList(broadcast.map((d) => d.channelId)),
        "",
        "__Morning Summary__",
        renderList(digest.map((d) => d.channelId)),
      ].join("\n");

      await editInteractionResponse(token, {
        components: [
          {
            type: ComponentType.Container,
            components: textDisplayComponents(body),
          },
        ],
        flags: MessageFlags.IsComponentsV2,
      });
      return;
    }

    // Every other branch operates on a channel option
    const channel = getResolvedChannel(interaction, "channel");
    if (channel == null) {
      await editInteractionResponse(token, { content: "No channel provided" });
      return;
    }

    // /setting default_forum channel:<forum>
    if (group == null && sub === "default_forum") {
      if (channel.type !== ChannelType.GuildForum) {
        await editInteractionResponse(token, {
          content: "Invalid channel type — must be a forum channel",
        });
        return;
      }
      const channelLabel =
        channel.name != null ? `\`${channel.name}\`` : `<#${channel.id}>`;
      const progress = new ProgressMessageBuilder(token).addStep(
        `Setting default forum to ${channelLabel}`,
      );
      await progress.sendNextStep();
      await setDefaultForum(channel.id, guildId);
      progress.setCurrentStatus(StepStatus.COMPLETE);
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\nDefault forum set to <#${channel.id}>`,
      });
      return;
    }

    // /setting global_episode_broadcast add|remove channel:<x>
    if (group === "global_episode_broadcast") {
      if (!TEXT_CHANNEL_TYPES.has(channel.type)) {
        await editInteractionResponse(token, {
          content:
            "Invalid channel type — must be a text or announcement channel",
        });
        return;
      }
      if (sub !== "add" && sub !== "remove") {
        await editInteractionResponse(token, { content: "Invalid mode" });
        return;
      }

      const existing = await getGlobalDestinations(
        "global_episode_broadcast",
        guildId,
      );
      const alreadyHas = existing.some((d) => d.channelId === channel.id);

      // Idempotent no-ops
      if (sub === "add" && alreadyHas) {
        await editInteractionResponse(token, {
          content: [
            `Already broadcasting to <#${channel.id}>. No change.`,
            "",
            "__Broadcasting to:__",
            renderList(existing.map((d) => d.channelId)),
          ].join("\n"),
        });
        return;
      }
      if (sub === "remove" && !alreadyHas) {
        await editInteractionResponse(token, {
          content: [
            `<#${channel.id}> wasn't on the broadcast list. No change.`,
            "",
            "__Broadcasting to:__",
            renderList(existing.map((d) => d.channelId)),
          ].join("\n"),
        });
        return;
      }

      const progress = new ProgressMessageBuilder(token).addStep(
        sub === "add"
          ? `Adding <#${channel.id}> to the broadcast list`
          : `Removing <#${channel.id}> from the broadcast list`,
      );
      await progress.sendNextStep();

      const destinations =
        sub === "add"
          ? await addGlobalDestination(
              "global_episode_broadcast",
              channel.id,
              guildId,
            )
          : await removeGlobalDestination(
              "global_episode_broadcast",
              channel.id,
              guildId,
            );

      const headline =
        sub === "add"
          ? `Now broadcasting episodes to <#${channel.id}>.`
          : `Stopped broadcasting to <#${channel.id}>.`;

      progress.setCurrentStatus(StepStatus.COMPLETE);
      await editInteractionResponse(token, {
        content: [
          `${progress.toString()}\n\n${headline}`,
          "",
          "__Broadcasting to:__",
          renderList(destinations.map((d) => d.channelId)),
        ].join("\n"),
      });
      return;
    }

    // /setting morning_summary add|remove channel:<x>
    if (group === "morning_summary") {
      if (!TEXT_CHANNEL_TYPES.has(channel.type)) {
        await editInteractionResponse(token, {
          content:
            "Invalid channel type — must be a text or announcement channel",
        });
        return;
      }
      if (sub !== "add" && sub !== "remove") {
        await editInteractionResponse(token, { content: "Invalid mode" });
        return;
      }

      const existing = await getGlobalDestinations("morning_summary", guildId);
      const alreadyHas = existing.some((d) => d.channelId === channel.id);

      if (sub === "add" && alreadyHas) {
        await editInteractionResponse(token, {
          content: [
            `Already sending the daily digest to <#${channel.id}>. No change.`,
            "",
            "__Digest going to:__",
            renderList(existing.map((d) => d.channelId)),
          ].join("\n"),
        });
        return;
      }
      if (sub === "remove" && !alreadyHas) {
        await editInteractionResponse(token, {
          content: [
            `<#${channel.id}> wasn't on the digest list. No change.`,
            "",
            "__Digest going to:__",
            renderList(existing.map((d) => d.channelId)),
          ].join("\n"),
        });
        return;
      }

      const progress = new ProgressMessageBuilder(token).addStep(
        sub === "add"
          ? `Adding <#${channel.id}> to the digest list`
          : `Removing <#${channel.id}> from the digest list`,
      );
      await progress.sendNextStep();

      const destinations =
        sub === "add"
          ? await addGlobalDestination("morning_summary", channel.id, guildId)
          : await removeGlobalDestination(
              "morning_summary",
              channel.id,
              guildId,
            );

      const headline =
        sub === "add"
          ? `Now sending the daily digest to <#${channel.id}>.`
          : `Stopped sending the daily digest to <#${channel.id}>.`;

      progress.setCurrentStatus(StepStatus.COMPLETE);
      await editInteractionResponse(token, {
        content: [
          `${progress.toString()}\n\n${headline}`,
          "",
          "__Digest going to:__",
          renderList(destinations.map((d) => d.channelId)),
        ].join("\n"),
      });
      return;
    }

    await editInteractionResponse(token, {
      content: "Unknown setting command",
    });
  }
}
