import {
  type APIApplicationCommandAutocompleteInteraction,
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { showSearchAutocomplete } from "../lib/autocomplete.js";
import { editInteractionResponse } from "../lib/discord.js";
import { getStringOption, getSubcommand } from "../lib/interactionOptions.js";
import { getAllShows, getShowByImdbId } from "../lib/shows.js";
import { getSeriesByImdbId } from "../lib/tvdb.js";
import { getUpcomingEpisodesEmbed } from "../lib/upcoming.js";
import type { Command } from "./index.js";

export default class UpcomingCommand implements Command {
  public readonly name = "upcoming";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "upcoming" as const,
      description: "Show upcoming episodes",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "all",
          description: "All upcoming episodes",
        },
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "here",
          description: "Upcoming for shows linked here",
        },
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "show",
          description: "Upcoming for a specific show",
          options: [
            {
              type: ApplicationCommandOptionType.String,
              name: "query",
              description: "Show name or IMDB ID",
              required: true,
              autocomplete: true,
            },
          ],
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const sub = getSubcommand(interaction);
    const token = interaction.token;

    if (sub === "all") {
      await this.handleAll(token);
      return;
    }
    if (sub === "here") {
      await this.handleHere(interaction);
      return;
    }
    if (sub === "show") {
      await this.handleShow(interaction);
      return;
    }

    await editInteractionResponse(token, { content: "Invalid subcommand" });
  }

  private async handleAll(token: string): Promise<void> {
    const allShows = await getAllShows();
    const showsWithUnsent = allShows.filter((s) =>
      s.episodes.some((e) => !e.messageSent),
    );

    if (showsWithUnsent.length === 0) {
      await editInteractionResponse(token, { content: "No shows found" });
      return;
    }

    const embed = getUpcomingEpisodesEmbed(showsWithUnsent, 7);
    await editInteractionResponse(token, { embeds: [embed] });
  }

  private async handleHere(
    interaction: APIApplicationCommandInteraction,
  ): Promise<void> {
    const channelId = interaction.channel?.id;
    const token = interaction.token;

    if (channelId == null) {
      await editInteractionResponse(token, {
        content: "Could not determine channel",
      });
      return;
    }

    const allShows = await getAllShows();
    const showsHere = allShows.filter((s) =>
      s.destinations.some((d) => d.channelId === channelId),
    );

    if (showsHere.length === 0) {
      await editInteractionResponse(token, {
        content: "No shows linked to this channel",
      });
      return;
    }

    const embed = getUpcomingEpisodesEmbed(showsHere, 7);
    await editInteractionResponse(token, { embeds: [embed] });
  }

  private async handleShow(
    interaction: APIApplicationCommandInteraction,
  ): Promise<void> {
    const query = getStringOption(interaction, "query") ?? "";
    const token = interaction.token;

    const imdbId = query.toLowerCase().startsWith("tt") ? query : undefined;

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
  }

  async autoComplete(
    interaction: APIApplicationCommandAutocompleteInteraction,
  ): Promise<Response> {
    return showSearchAutocomplete(interaction);
  }
}
