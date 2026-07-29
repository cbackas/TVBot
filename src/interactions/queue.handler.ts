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

  try {
    await command.handler(interaction);
    logger.debug(
      `Finished command ${invocation} for user ${interaction.member?.user.username} [interaction ${interaction.id}]`,
      fields,
    );
  } catch (error) {
    logger.error(`Error executing command ${invocation}`, {
      ...fields,
      error,
    });
    await editInteractionResponse(interaction.token, {
      content: "There was an error while executing this command!",
    });
  }
}
