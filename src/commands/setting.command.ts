import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { editInteractionResponse } from "../lib/discord.js";
import {
  getResolvedChannel,
  getSubcommand,
  getSubcommandGroup,
} from "../lib/interactionOptions.js";
import { ProgressMessageBuilder } from "../lib/progressMessages.js";
import {
  addGlobalDestination,
  getGlobalDestinations,
  removeGlobalDestination,
  setDefaultForum,
} from "../lib/settingsManager.js";
import type { Command } from "./index.js";

const TEXT_CHANNEL_TYPES = new Set<number>([
  ChannelType.GuildText,
  ChannelType.GuildAnnouncement,
]);

export default class SettingCommand implements Command {
  public readonly name = "setting";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "setting" as const,
      description: "Bot settings",
      default_member_permissions: "16",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "tv_forum",
          description: "Set the default TV forum",
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
          name: "all_episodes",
          description: "All-episodes notification channels",
          options: [
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "add",
              description: "Add a channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Channel",
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
              description: "Remove a channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Channel",
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
          description: "Morning summary channels",
          options: [
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "add_channel",
              description: "Add a channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Channel",
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
              name: "remove_channel",
              description: "Remove a channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Channel",
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
    const channel = getResolvedChannel(interaction, "channel");

    if (channel == null) {
      await editInteractionResponse(token, { content: "No channel provided" });
      return;
    }

    // /setting tv_forum <channel>
    if (group == null && sub === "tv_forum") {
      if (channel.type !== ChannelType.GuildForum) {
        await editInteractionResponse(token, {
          content: "Invalid channel type — must be a forum channel",
        });
        return;
      }
      const channelLabel =
        channel.name != null ? `\`${channel.name}\`` : `<#${channel.id}>`;
      const progress = new ProgressMessageBuilder(token).addStep(
        `Setting TV forum to ${channelLabel}`,
      );
      await progress.sendNextStep();
      await setDefaultForum(channel.id);
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\nTV forum set to <#${channel.id}>`,
      });
      return;
    }

    // /setting all_episodes add/remove <channel>
    if (group === "all_episodes") {
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
      const progress = new ProgressMessageBuilder(token).addStep(
        "Updating All Episodes channel list",
      );
      await progress.sendNextStep();
      const destinations =
        sub === "add"
          ? await addGlobalDestination("all_episodes", channel.id)
          : await removeGlobalDestination("all_episodes", channel.id);
      const list = destinations.map((d) => `<#${d.channelId}>`).join("\n");
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\nUpdated All Episodes channel list.\n\n__New List__:\n${list}`,
      });
      return;
    }

    // /setting morning_summary add_channel/remove_channel <channel>
    if (group === "morning_summary") {
      if (!TEXT_CHANNEL_TYPES.has(channel.type)) {
        await editInteractionResponse(token, {
          content:
            "Invalid channel type — must be a text or announcement channel",
        });
        return;
      }
      if (sub !== "add_channel" && sub !== "remove_channel") {
        await editInteractionResponse(token, { content: "Invalid mode" });
        return;
      }
      const existing = await getGlobalDestinations("morning_summary");
      const alreadyHas = existing.some((d) => d.channelId === channel.id);

      if (sub === "add_channel" && alreadyHas) {
        await editInteractionResponse(token, {
          content: "Channel already in list",
        });
        return;
      }
      if (sub === "remove_channel" && !alreadyHas) {
        await editInteractionResponse(token, {
          content: "Channel not in morning_summary list",
        });
        return;
      }

      const progress = new ProgressMessageBuilder(token).addStep(
        "Updating Morning Summary channel list",
      );
      await progress.sendNextStep();
      const destinations =
        sub === "add_channel"
          ? await addGlobalDestination("morning_summary", channel.id)
          : await removeGlobalDestination("morning_summary", channel.id);
      const list = destinations.map((d) => `<#${d.channelId}>`).join("\n");
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\nUpdated Morning Summary channel list.\n\n__New List__:\n${list}`,
      });
      return;
    }

    await editInteractionResponse(token, {
      content: "Unknown setting command",
    });
  }
}
