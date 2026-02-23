import {
  type APIApplicationCommandAutocompleteInteraction,
  type APIApplicationCommandInteractionDataOption,
  ApplicationCommandOptionType,
} from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { searchShows } from "./shows.js";

/**
 * Recursively find the focused option, unwrapping subcommand groups and subcommands.
 */
function findFocusedOption(
  options: APIApplicationCommandInteractionDataOption[],
): APIApplicationCommandInteractionDataOption | undefined {
  for (const opt of options) {
    if ("focused" in opt && opt.focused) return opt;
    if (
      (opt.type === ApplicationCommandOptionType.SubcommandGroup ||
        opt.type === ApplicationCommandOptionType.Subcommand) &&
      "options" in opt &&
      opt.options
    ) {
      const found = findFocusedOption(opt.options);
      if (found) return found;
    }
  }
  return undefined;
}

export async function showSearchAutocomplete(
  interaction: APIApplicationCommandAutocompleteInteraction,
): Promise<Response> {
  const focused = findFocusedOption(interaction.data.options ?? []);
  const focusedValue =
    focused && "value" in focused ? String(focused.value) : "";

  const data = await searchShows(focusedValue);

  const choices = data.map((item) => ({
    name: `${item.name} - (${item.imdbId})`,
    value: item.imdbId,
  }));

  return Response.json({
    type: InteractionResponseType.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT,
    data: { choices },
  });
}
