import { InteractionResponseType } from "discord-interactions";

export default function handlePing() {
  return new Response(JSON.stringify({ type: InteractionResponseType.PONG }), {
    headers: { "Content-Type": "application/json" },
  });
}
