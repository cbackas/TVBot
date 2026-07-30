import { env } from "cloudflare:workers";
import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import {
  InteractionResponseFlags,
  InteractionResponseType,
} from "discord-interactions";
import { commands } from "../commands/index.js";
import {
  formatCommandInvocation,
  formatUser,
} from "../lib/interactionOptions.js";
import { addLogContext } from "../lib/logger.js";

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

  const invocation = formatCommandInvocation(interaction, command.definition);

  // Pin command/user metadata onto the interaction's log context so it rides
  // along with every log from here through the queued execution.
  addLogContext({
    commandName: interaction.data.name,
    commandInput: invocation,
    userId: interaction.member?.user.id,
    username: interaction.member?.user.username,
    guildId: interaction.guild_id,
    channelId: interaction.channel.id,
  });

  console.info(`Queuing ${invocation} from ${formatUser(interaction)}`);

  await env.WORK_QUEUE.send({ type: "command", interaction });

  return Response.json({
    type: InteractionResponseType.DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE,
  });
}
