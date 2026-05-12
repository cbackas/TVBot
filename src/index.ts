import { type APIInteraction, InteractionType } from "discord-api-types/v10";
import handleAutocomplete from "./interactions/autocomplete.handler.js";
import handleCommand from "./interactions/command.handler.js";
import handleComponent from "./interactions/component.handler.js";
import handlePing from "./interactions/ping.handler.js";
import {
  handleQueuedWork,
  type WorkQueueMessage,
} from "./interactions/queue.handler.js";
import { verifyInteraction } from "./interactions/verify.js";
import { getEnv } from "./lib/env.js";
import { sendAiringMessages } from "./lib/episodeNotifier.js";
import { sendMorningSummary } from "./lib/morningSummary.js";
import {
  checkForAiringEpisodes,
  pruneUnsubscribedShows,
  sweepDeadChannels,
} from "./lib/shows.js";

const textDecoder = new TextDecoder();

async function fetch(
  request: Request,
  _env: Env,
  _ctx: ExecutionContext,
): Promise<Response> {
  if (request.method !== "POST") {
    return new Response("OK");
  }

  const body = await request.arrayBuffer();

  const verification = await verifyInteraction(body, request.headers);
  if (verification.valid === false) {
    return new Response(verification.error, { status: verification.code });
  }

  const interaction: APIInteraction = JSON.parse(textDecoder.decode(body));
  try {
    switch (interaction.type) {
      case InteractionType.Ping: {
        return handlePing();
      }
      case InteractionType.ApplicationCommand: {
        return await handleCommand(interaction);
      }
      case InteractionType.ApplicationCommandAutocomplete: {
        return await handleAutocomplete(interaction);
      }
      case InteractionType.MessageComponent: {
        return await handleComponent(interaction);
      }
    }
  } catch (error) {
    console.error("Error handling interaction:", error);
    return new Response("Internal Server Error", { status: 500 });
  }

  return new Response("Not implemented", { status: 200 });
}

async function scheduled(
  controller: ScheduledController,
  _env: Env,
  _ctx: ExecutionContext,
): Promise<void> {
  switch (controller.cron) {
    case "*/2 * * * *":
      await sendAiringMessages();
      return;
    case "0 */4 * * *":
      await checkForAiringEpisodes();
      return;
    case "0 5 * * *":
      await pruneUnsubscribedShows();
      await sweepDeadChannels(getEnv("DISCORD_TOKEN"));
      return;
    case "0 8 * * *":
      await sendMorningSummary();
      return;
    default:
      console.warn(`Unhandled cron trigger: ${controller.cron}`);
  }
}

async function queue(
  batch: MessageBatch<WorkQueueMessage>,
  _env: Env,
  _ctx: ExecutionContext,
): Promise<void> {
  for (const message of batch.messages) {
    try {
      await handleQueuedWork(message.body);
    } catch (error) {
      console.error("Unhandled error in queue handler:", error);
    } finally {
      message.ack();
    }
  }
}

export default {
  fetch,
  scheduled,
  queue,
} satisfies ExportedHandler<Env, WorkQueueMessage>;
