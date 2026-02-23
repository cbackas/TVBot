import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
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
