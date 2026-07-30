import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import { commands } from "../commands/index.js";
import { editInteractionResponse } from "../lib/discord.js";
import {
  formatCommandInvocation,
  formatUser,
} from "../lib/interactionOptions.js";
import { addLogContext, runWithLogContext } from "../lib/logger.js";

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

  // Fresh context for this queue invocation. The interaction id matches the one
  // logged when the command was queued, so the two halves correlate.
  await runWithLogContext(
    {
      interactionId: interaction.id,
      commandName: interaction.data.name,
      commandInput: invocation,
      userId: interaction.member?.user.id,
      username: interaction.member?.user.username,
      guildId: interaction.guild_id,
      channelId: interaction.channel?.id,
    },
    async () => {
      console.info(`Executing ${invocation} from ${formatUser(interaction)}`);

      const start = Date.now();
      try {
        await command.handler(interaction);
        addLogContext({ durationMs: Date.now() - start });
        console.debug(
          `Finished ${invocation} in ${((Date.now() - start) / 1000).toFixed(1)}s`,
        );
      } catch (error) {
        addLogContext({ durationMs: Date.now() - start });
        console.error(`Failed ${invocation}`, error);
        await editInteractionResponse(interaction.token, {
          content: "There was an error while executing this command!",
        });
      }
    },
  );
}
