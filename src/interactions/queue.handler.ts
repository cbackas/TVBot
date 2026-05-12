import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import { commands } from "../commands/index.js";
import { editInteractionResponse } from "../lib/discord.js";

export type WorkQueueMessage = {
  type: "command";
  interaction: APIApplicationCommandInteraction;
};

export async function handleQueuedWork(msg: WorkQueueMessage): Promise<void> {
  if (msg.type !== "command") return;

  const { interaction } = msg;
  const command = commands.get(interaction.data.name);
  if (!command) {
    await editInteractionResponse(interaction.token, {
      content: `Unknown command: ${interaction.data.name}`,
    });
    return;
  }

  console.info(
    `Executing queued command ${interaction.data.name} for user ${interaction.member?.user.username} (${interaction.member?.user.id})`,
  );

  try {
    await command.handler(interaction);
    console.debug(
      `Finished command ${interaction.data.name} for user ${interaction.member?.user.username}`,
    );
  } catch (error) {
    console.error(`Error executing command ${interaction.data.name}:`, error);
    await editInteractionResponse(interaction.token, {
      content: "There was an error while executing this command!",
    });
  }
}
