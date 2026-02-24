import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { editInteractionResponse } from "../lib/discord.js";
import {
  getChannelOption,
  getSubcommand,
  getSubcommandGroup,
} from "../lib/interactionOptions.js";
import { getAllShows } from "../lib/shows.js";
import type { Command } from "./index.js";

export default class ListCommand implements Command {
  public readonly name = "list";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "list" as const,
      description: "List linked shows",
      default_member_permissions: "16",
      options: [
        {
          type: ApplicationCommandOptionType.SubcommandGroup,
          name: "shows",
          description: "List shows",
          options: [
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "here",
              description: "Shows linked to this channel",
            },
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "channel",
              description: "Shows linked to a specific channel",
              options: [
                {
                  type: ApplicationCommandOptionType.Channel,
                  name: "channel",
                  description: "Target channel",
                  required: true,
                  channel_types: [ChannelType.GuildText],
                },
              ],
            },
          ],
        },
      ],
    };

  async handler(
    interaction: APIApplicationCommandInteraction,
  ): Promise<void> {
    const group = getSubcommandGroup(interaction);
    const sub = getSubcommand(interaction);

    if (group !== "shows") {
      await editInteractionResponse(interaction.token, {
        content: "Invalid subcommand group",
      });
      return;
    }

    let channelId: string | null = null;
    if (sub === "here") {
      channelId = interaction.channel?.id ?? null;
    } else if (sub === "channel") {
      channelId = getChannelOption(interaction, "channel");
    }

    if (channelId == null) {
      await editInteractionResponse(interaction.token, {
        content: "Invalid channel",
      });
      return;
    }

    const allShows = await getAllShows();
    const showsInChannel = allShows.filter((show) =>
      show.destinations.some((d) => d.channelId === channelId),
    );

    if (showsInChannel.length === 0) {
      await editInteractionResponse(interaction.token, {
        content: `No shows linked to <#${channelId}>`,
      });
      return;
    }

    const MAX_LENGTH = 2000;
    const header = `Shows in channel <#${channelId}>:\n\n`;
    let content = header;
    let added = 0;

    for (let i = 0; i < showsInChannel.length; i++) {
      const show = showsInChannel[i];
      const destinations = show.destinations
        .map((d) => `<#${d.channelId}>`)
        .join(" ");
      const line = `**${show.name}** ${destinations}`;
      const remaining = showsInChannel.length - added;
      const suffix = `\n...and ${remaining} more`;

      if (
        content.length + (added > 0 ? 1 : 0) + line.length + suffix.length >
        MAX_LENGTH
      ) {
        content += suffix;
        break;
      }

      if (added > 0) content += "\n";
      content += line;
      added++;
    }

    await editInteractionResponse(interaction.token, { content });
  }
}
