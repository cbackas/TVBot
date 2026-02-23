import type {
  APIApplicationCommandAutocompleteInteraction,
  APIApplicationCommandInteraction,
} from "discord-api-types/v10";
import { showSearchAutocomplete } from "../lib/autocomplete.js";
import { deferWithWork, editInteractionResponse } from "../lib/discord.js";
import { getStringOption } from "../lib/interactionOptions.js";
import { buildShowEmbed } from "../lib/messages.js";
import { getShowByImdbId } from "../lib/shows.js";
import { getSeriesByImdbId, getSeriesByName } from "../lib/tvdb.js";
import type { Command } from "./index.js";

export default class SearchCommand implements Command {
  public readonly name = "search";

  async handler(
    interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    const query = getStringOption(interaction, "query") ?? "";
    const token = interaction.token;

    return deferWithWork(
      (async () => {
        try {
          let imdbId = query.toLowerCase().startsWith("tt") ? query : undefined;

          if (imdbId !== undefined) {
            const series = await getSeriesByImdbId(imdbId);
            if (series === undefined) {
              await editInteractionResponse(token, {
                content: `No show found with IMDB ID \`${imdbId}\``,
              });
              return;
            }

            const show = await getShowByImdbId(imdbId);
            await editInteractionResponse(token, {
              embeds: [
                buildShowEmbed(imdbId, series, show?.destinations ?? []),
              ],
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
          await editInteractionResponse(token, {
            content: "An error occurred while searching.",
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
