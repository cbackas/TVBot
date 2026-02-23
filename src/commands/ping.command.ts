import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import type { Command } from "./index.js";

export default class PingCommand implements Command {
  public readonly name = "ping";

  async handler(
    _interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    return Response.json({
      type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
      data: { content: "Pong!" },
    });
  }
}
