import type { APIMessageComponentInteraction } from "discord-api-types/v10";
import {
  InteractionResponseFlags,
  InteractionResponseType,
} from "discord-interactions";
import { commands } from "../commands/index.js";

export default async function handleComponent(
  interaction: APIMessageComponentInteraction,
) {
  for (const command of commands.values()) {
    if (command.selectMenuIds?.includes(interaction.data.custom_id)) {
      if (!command.componentHandler) break;
      try {
        return await command.componentHandler(interaction);
      } catch (error) {
        console.error(
          `Error handling component ${interaction.data.custom_id}:`,
          error,
        );
        return Response.json({
          type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
          data: {
            content: "There was an error while handling this interaction!",
            flags: InteractionResponseFlags.EPHEMERAL,
          },
        });
      }
    }
  }

  return Response.json({
    type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
    data: {
      content: "Unknown component interaction.",
      flags: InteractionResponseFlags.EPHEMERAL,
    },
  });
}
