import {
  type APIApplicationCommandAutocompleteInteraction,
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { showSearchAutocomplete } from "../lib/autocomplete.js";
import { editInteractionResponse } from "../lib/discord.js";
import { getStringOption } from "../lib/interactionOptions.js";
import { buildShowEmbed } from "../lib/messages.js";
import { getShowByImdbId } from "../lib/shows.js";
import { getSeriesByImdbId, getSeriesByName } from "../lib/tvdb.js";
import type { Command } from "./index.js";

export default class SearchCommand implements Command {
  public readonly name = "search";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "search" as const,
      description: "Search for a TV show",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.String,
          name: "query",
          description: "Show name or IMDB ID",
          required: true,
          autocomplete: true,
          min_length: 1,
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const query = getStringOption(interaction, "query") ?? "";
    const token = interaction.token;

    try {
      let imdbId = query.toLowerCase().startsWith("tt") ? query : undefined;

      if (imdbId !== undefined) {
        const series = await getSeriesByImdbId(imdbId);
        if (series === undefined) {
          await editInteractionResponse(token, {
            content: `No TVDB match for IMDB ID \`${imdbId}\``,
          });
          return;
        }

        const show = await getShowByImdbId(imdbId);
        await editInteractionResponse(token, {
          embeds: [buildShowEmbed(imdbId, series, show?.destinations ?? [])],
        });
        return;
      }

      const series = await getSeriesByName(query);
      if (series == null) {
        await editInteractionResponse(token, {
          content: "Show not found",
        });
        return;
      }

      imdbId = series.remoteIds.find((r) => r.type === 2)?.id;
      if (imdbId == null) {
        await editInteractionResponse(token, {
          content: "Show not found",
        });
        return;
      }

      const show = await getShowByImdbId(imdbId);
      await editInteractionResponse(token, {
        embeds: [buildShowEmbed(imdbId, series, show?.destinations ?? [])],
      });
    } catch (error) {
      console.error("Error in search command:", error);
      const errorMessage =
        error instanceof Error ? error.message : String(error);
      await editInteractionResponse(token, {
        content: `An error occurred while searching: ${errorMessage}`,
      });
    }
  }

  async autoComplete(
    interaction: APIApplicationCommandAutocompleteInteraction,
  ): Promise<Response> {
    return showSearchAutocomplete(interaction);
  }
}
