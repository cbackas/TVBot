import type {
  APIApplicationCommandInteraction,
  APIMessageComponentInteraction,
} from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { getChannelOption, getSubcommand } from "../lib/interactionOptions.js";
import {
  getAllShows,
  pruneUnsubscribedShows,
  removeSubscription,
} from "../lib/shows.js";
import type { Command } from "./index.js";

export default class UnlinkCommand implements Command {
  public readonly name = "unlink";
  public readonly selectMenuIds = ["unlink_shows_menu"];

  async handler(
    interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    const sub = getSubcommand(interaction);

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
        data: { content: "This channel has no shows linked to it." },
      });
    }

    const options = showsInChannel.map((s) => ({
      label: s.name,
      value: s.imdbId,
    }));

    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: {
        content: `Select the shows that you'd like to unlink from <#${channelId}>`,
        components: [
          {
            type: 1, // ActionRow
            components: [
              {
                type: 3, // StringSelect
                custom_id: "unlink_shows_menu",
                placeholder: "Nothing selected",
                min_values: 1,
                max_values: options.length,
                options,
              },
            ],
          },
        ],
      },
    });
  }

  async componentHandler(
    interaction: APIMessageComponentInteraction,
  ): Promise<Response> {
    const channelId = interaction.message?.content?.match(/<#([0-9]+)>/)?.[1];

    if (channelId == null) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "Failed to find channel" },
      });
    }

    const values = "values" in interaction.data ? interaction.data.values : [];

    let count = 0;
    for (const imdbId of values) {
      try {
        await removeSubscription(imdbId, channelId);
        count++;
      } catch (error) {
        console.error(`Failed to unlink ${imdbId}:`, error);
      }
    }

    await pruneUnsubscribedShows();

    return Response.json({
      type: InteractionResponseType.UPDATE_MESSAGE,
      data: {
        content: `Unlinked ${count} shows from <#${channelId}>`,
        components: [],
      },
    });
  }
}
