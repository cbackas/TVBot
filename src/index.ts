import { type APIInteraction, InteractionType } from "discord-api-types/v10";
import handleAutocomplete from "./interactions/autocomplete.handler.js";
import handleCommand from "./interactions/command.handler.js";
import handleComponent from "./interactions/component.handler.js";
import handlePing from "./interactions/ping.handler.js";
import { verifyInteraction } from "./interactions/verify.js";

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

  return new Response("Not implemented", { status: 200 });
}

export default {
  fetch,
} satisfies ExportedHandler<Env>;
