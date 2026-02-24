import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import {
  InteractionResponseFlags,
  InteractionResponseType,
} from "discord-interactions";
import { commands } from "../commands/index.js";
import { deferWithWork, editInteractionResponse } from "../lib/discord.js";

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

  console.info(
    `Executing command ${interaction.data.name} for user ${interaction.member?.user.username} (${interaction.member?.user.id}) in channel ${interaction.channel.id} of guild ${interaction.guild_id}`,
  );

  return deferWithWork(
    (async () => {
      try {
        await command.handler(interaction);
      } catch (error) {
        console.error(
          `Error executing command ${interaction.data.name}:`,
          error,
        );
        await editInteractionResponse(interaction.token, {
          content: "There was an error while executing this command!",
        });
      }
    })(),
  );
}
