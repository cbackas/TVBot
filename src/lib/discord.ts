import { waitUntil } from "cloudflare:workers";
import { type APIEmbed, ComponentType } from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { getEnv } from "./env.js";
import { pruneDeadChannel } from "./shows.js";

/**
 * Translate a non-ok Discord channel/message response into a thrown error,
 * self-healing when Discord reports the channel itself no longer exists (10003).
 * Other 404s (e.g. 50001 missing access) bubble up as plain errors — they may
 * be a recoverable permission misconfig, not a deletion.
 */
export async function handleChannelSendError(
  response: Response,
  channelId: string,
  operation: string,
): Promise<never> {
  const errorText = await response.text();
  if (response.status === 404) {
    let code: number | undefined;
    try {
      code = (JSON.parse(errorText) as { code?: number }).code;
    } catch {
      // not JSON, treat as opaque 404
    }
    if (code === 10003) {
      await pruneDeadChannel(channelId);
      throw new Error(
        `${operation} ${channelId} — channel no longer exists; pruned from DB`,
      );
    }
  }
  throw new Error(
    `${operation} ${channelId} — Discord API error ${response.status}: ${errorText}`,
  );
}

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
  data: {
    content?: string;
    embeds?: APIEmbed[];
    components?: unknown[];
    flags?: number;
  },
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

export function textDisplayComponents(
  content: string,
  options: { chunk?: boolean; maxComponents?: number } = {},
): { type: ComponentType.TextDisplay; content: string }[] {
  const MAX = 4000;

  if (!options.chunk) {
    // Single TextDisplay, truncated at 4000 chars
    return [
      { type: ComponentType.TextDisplay, content: content.slice(0, MAX) },
    ];
  }

  // Split content by newlines, pack into ≤4000-char TextDisplays
  const lines = content.split("\n");
  const displays: { type: ComponentType.TextDisplay; content: string }[] = [];
  let currentChunk = "";

  for (const line of lines) {
    if (currentChunk && currentChunk.length + 1 + line.length > MAX) {
      displays.push({ type: ComponentType.TextDisplay, content: currentChunk });
      currentChunk = line;
    } else {
      currentChunk = currentChunk ? `${currentChunk}\n${line}` : line;
    }
  }
  if (currentChunk) {
    displays.push({ type: ComponentType.TextDisplay, content: currentChunk });
  }

  // Truncate if we exceed the component limit
  if (options.maxComponents && displays.length > options.maxComponents) {
    const kept = displays.slice(0, options.maxComponents);
    const remaining =
      lines.length -
      kept.reduce((n, td) => n + td.content.split("\n").length, 0);
    kept[kept.length - 1].content += `\n...and ${remaining} more`;
    return kept;
  }

  return displays;
}
