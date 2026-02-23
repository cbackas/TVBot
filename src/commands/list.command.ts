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
import { getAllShows } from "../lib/shows.js";
import type { Command } from "./index.js";

export default class ListCommand implements Command {
  public readonly name = "list";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "list" as const,
      description: "List linked shows",
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

    if (group !== "shows") {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "Invalid subcommand group" },
      });
    }

    let channelId: string | null = null;
    if (sub === "here") {
      channelId = interaction.channel?.id ?? null;
    } else if (sub === "channel") {
      channelId = getChannelOption(interaction, "channel");
    }

    if (channelId == null) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "Invalid channel" },
      });
    }

    const allShows = await getAllShows();
    const showsInChannel = allShows.filter((show) =>
      show.destinations.some((d) => d.channelId === channelId),
    );

    if (showsInChannel.length === 0) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: {
          content: `No shows linked to <#${channelId}>`,
        },
      });
    }

    const showMessages = showsInChannel
      .map((show) => {
        const destinations = show.destinations
          .map((d) => `<#${d.channelId}>`)
          .join(" ");
        return `**${show.name}** ${destinations}`;
      })
      .join("\n");

    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: {
        content: `Shows in channel <#${channelId}>:\n\n${showMessages}`,
      },
    });
  }
}
