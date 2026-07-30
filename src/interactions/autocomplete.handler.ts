import type { APIApplicationCommandAutocompleteInteraction } from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { commands } from "../commands/index.js";

const emptyChoices = () =>
  Response.json({
    type: InteractionResponseType.APPLICATION_COMMAND_AUTOCOMPLETE_RESULT,
    data: { choices: [] },
  });

export default async function handleAutocomplete(
  interaction: APIApplicationCommandAutocompleteInteraction,
) {
  const command = commands.get(interaction.data.name);
  if (!command?.autoComplete) {
    return emptyChoices();
  }

  try {
    return await command.autoComplete(interaction);
  } catch (error) {
    console.error(`Error in autocomplete for ${interaction.data.name}:`, error);
    return emptyChoices();
  }
}
