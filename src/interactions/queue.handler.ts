import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import { commands } from "../commands/index.js";
import { editInteractionResponse } from "../lib/discord.js";
import { formatCommandInvocation } from "../lib/interactionOptions.js";
import { logger } from "../lib/logger.js";

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

  const invocation = formatCommandInvocation(interaction, command.definition);
  const fields = {
    interactionId: interaction.id,
    commandName: interaction.data.name,
    commandInput: invocation,
    userId: interaction.member?.user.id,
    username: interaction.member?.user.username,
    guildId: interaction.guild_id,
    channelId: interaction.channel?.id,
  };

  logger.info(
    `Executing queued command ${invocation} for user ${interaction.member?.user.username} (${interaction.member?.user.id}) [interaction ${interaction.id}]`,
    fields,
  );

  const start = Date.now();
  try {
    await command.handler(interaction);
    const durationMs = Date.now() - start;
    logger.debug(
      `Finished command ${invocation} for user ${interaction.member?.user.username} in ${(durationMs / 1000).toFixed(1)}s [interaction ${interaction.id}]`,
      { ...fields, durationMs },
    );
  } catch (error) {
    const durationMs = Date.now() - start;
    logger.error(
      `Error executing command ${invocation} after ${(durationMs / 1000).toFixed(1)}s`,
      {
        ...fields,
        durationMs,
        error,
      },
    );
    await editInteractionResponse(interaction.token, {
      content: "There was an error while executing this command!",
    });
  }
}
