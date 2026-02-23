import type {
  APIApplicationCommandAutocompleteInteraction,
  APIApplicationCommandInteraction,
} from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { showSearchAutocomplete } from "../lib/autocomplete.js";
import { deferWithWork, editInteractionResponse } from "../lib/discord.js";
import { getStringOption, getSubcommand } from "../lib/interactionOptions.js";
import { getAllShows, getShowByImdbId } from "../lib/shows.js";
import { getSeriesByImdbId } from "../lib/tvdb.js";
import { getUpcomingEpisodesEmbed } from "../lib/upcoming.js";
import type { Command } from "./index.js";

export default class UpcomingCommand implements Command {
  public readonly name = "upcoming";

  async handler(
    interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    const sub = getSubcommand(interaction);

    if (sub === "all") {
      return this.handleAll();
    }
    if (sub === "here") {
      return this.handleHere(interaction);
    }
    if (sub === "show") {
      return this.handleShow(interaction);
    }

    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: { content: "Invalid subcommand" },
    });
  }

  private async handleAll(): Promise<Response> {
    const allShows = await getAllShows();
    const showsWithUnsent = allShows.filter((s) =>
      s.episodes.some((e) => !e.messageSent),
    );

    if (showsWithUnsent.length === 0) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "No shows found" },
      });
    }

    const embed = getUpcomingEpisodesEmbed(showsWithUnsent, 7);
    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: { embeds: [embed] },
    });
  }

  private async handleHere(
    interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    const channelId = interaction.channel?.id;
    if (channelId == null) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "Could not determine channel" },
      });
    }

    const allShows = await getAllShows();
    const showsHere = allShows.filter((s) =>
      s.destinations.some((d) => d.channelId === channelId),
    );

    if (showsHere.length === 0) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "No shows linked to this channel" },
      });
    }

    const embed = getUpcomingEpisodesEmbed(showsHere, 7);
    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: { embeds: [embed] },
    });
  }

  private handleShow(interaction: APIApplicationCommandInteraction): Response {
    const query = getStringOption(interaction, "query") ?? "";
    const token = interaction.token;

    return deferWithWork(
      (async () => {
        try {
          const imdbId = query.toLowerCase().startsWith("tt")
            ? query
            : undefined;

          if (imdbId == null) {
            await editInteractionResponse(token, {
              content: "Invalid query — use the autocomplete to select a show",
            });
            return;
          }

          const series = await getSeriesByImdbId(imdbId);
          if (series == null) {
            await editInteractionResponse(token, {
              content: `No show found with IMDB ID \`${imdbId}\``,
            });
            return;
          }

          const show = await getShowByImdbId(imdbId);
          if (show == null) {
            await editInteractionResponse(token, {
              content: `${series.name} is not linked to any channels. Use \`/link\` or \`/post\` to subscribe a channel to episode notifications.`,
            });
            return;
          }

          const embed = getUpcomingEpisodesEmbed([show], 7);
          await editInteractionResponse(token, { embeds: [embed] });
        } catch (error) {
          console.error("Error in upcoming show command:", error);
          await editInteractionResponse(token, {
            content: "An error occurred while fetching upcoming episodes.",
          });
        }
      })(),
    );
  }

  async autoComplete(
    interaction: APIApplicationCommandAutocompleteInteraction,
  ): Promise<Response> {
    return showSearchAutocomplete(interaction);
  }
}
