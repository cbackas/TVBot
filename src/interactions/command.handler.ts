import { env } from "cloudflare:workers";
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

  console.info(
    `Queuing command ${interaction.data.name} for user ${interaction.member?.user.username} (${interaction.member?.user.id}) in channel ${interaction.channel.id} of guild ${interaction.guild_id}`,
  );

  await env.WORK_QUEUE.send({ type: "command", interaction });

  return Response.json({
    type: InteractionResponseType.DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE,
  });
}
