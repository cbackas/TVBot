import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import {
  getChannelOption,
  getSubcommand,
  getSubcommandGroup,
} from "../lib/interactionOptions.js";
import {
  addGlobalDestination,
  removeGlobalDestination,
  setDefaultForum,
} from "../lib/settingsManager.js";
import type { Command } from "./index.js";

export default class SettingCommand implements Command {
  public readonly name = "setting";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "setting" as const,
      description: "Bot settings",
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
                },
              ],
            },
          ],
        },
      ],
    };

  async handler(
    interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    const group = getSubcommandGroup(interaction);
    const sub = getSubcommand(interaction);
    const channelId = getChannelOption(interaction, "channel");

    if (channelId == null) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "No channel provided" },
      });
    }

    // /setting tv_forum <channel>
    if (group == null && sub === "tv_forum") {
      await setDefaultForum(channelId);
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: `TV forum set to <#${channelId}>` },
      });
    }

    // /setting all_episodes add/remove <channel>
    if (group === "all_episodes") {
      if (sub === "add") {
        const destinations = await addGlobalDestination(
          "all_episodes",
          channelId,
        );
        const list = destinations.map((d) => `<#${d.channelId}>`).join("\n");
        return Response.json({
          type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
          data: {
            content: `Updated All Episodes channel list.\n\n__New List__:\n${list}`,
          },
        });
      }
      if (sub === "remove") {
        const destinations = await removeGlobalDestination(
          "all_episodes",
          channelId,
        );
        const list = destinations.map((d) => `<#${d.channelId}>`).join("\n");
        return Response.json({
          type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
          data: {
            content: `Updated All Episodes channel list.\n\n__New List__:\n${list}`,
          },
        });
      }
    }

    // /setting morning_summary add_channel/remove_channel <channel>
    if (group === "morning_summary") {
      if (sub === "add_channel") {
        const destinations = await addGlobalDestination(
          "morning_summary",
          channelId,
        );
        const list = destinations.map((d) => `<#${d.channelId}>`).join("\n");
        return Response.json({
          type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
          data: {
            content: `Updated Morning Summary channel list.\n\n__New List__:\n${list}`,
          },
        });
      }
      if (sub === "remove_channel") {
        const destinations = await removeGlobalDestination(
          "morning_summary",
          channelId,
        );
        const list = destinations.map((d) => `<#${d.channelId}>`).join("\n");
        return Response.json({
          type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
          data: {
            content: `Updated Morning Summary channel list.\n\n__New List__:\n${list}`,
          },
        });
      }
    }

    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: { content: "Unknown setting command" },
    });
  }
}
