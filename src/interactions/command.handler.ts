import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import {
  InteractionResponseFlags,
  InteractionResponseType,
} from "discord-interactions";
import { commands } from "../commands/index.js";

export default async function handleCommand(
  interaction: APIApplicationCommandInteraction,
) {
  const command = commands.get(interaction.data.name);
  if (!command) {
    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: {
        content: `Unknown command: ${interaction.data.name}`,
        flags: InteractionResponseFlags.EPHEMERAL,
      },
    });
  }

  try {
    return await command.handler(interaction);
  } catch (error) {
    console.error(`Error executing command ${interaction.data.name}:`, error);
    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: {
        content: "There was an error while executing this command!",
        flags: InteractionResponseFlags.EPHEMERAL,
      },
    });
  }
}
