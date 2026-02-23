import { waitUntil } from "cloudflare:workers";
import type { APIEmbed } from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { getEnv } from "./env.js";

/**
 * Returns a deferred response and schedules async work via waitUntil.
 * Use for commands that need >3s (e.g. TVDB API calls).
 */
export function deferWithWork(work: Promise<void>): Response {
  waitUntil(work);
  return Response.json({
    type: InteractionResponseType.DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE,
  });
}

/**
 * Edit the original interaction response (for deferred commands).
 * PATCH /webhooks/{app_id}/{token}/messages/@original
 */
export async function editInteractionResponse(
  interactionToken: string,
  data: { content?: string; embeds?: APIEmbed[]; components?: unknown[] },
): Promise<void> {
  const applicationId = getEnv("DISCORD_CLIENT_ID");
  const response = await fetch(
    `https://discord.com/api/v10/webhooks/${applicationId}/${interactionToken}/messages/@original`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    },
  );
  if (!response.ok) {
    console.error(
      `Failed to edit interaction response: ${response.status}`,
      await response.text(),
    );
  }
}
