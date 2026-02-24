import type {
  APIApplicationCommandInteraction,
  RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { editInteractionResponse } from "../lib/discord.js";
import type { Command } from "./index.js";

export default class PingCommand implements Command {
  public readonly name = "ping";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "ping" as const,
      description: "Check if the bot is alive",
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    await editInteractionResponse(interaction.token, { content: "Pong!" });
  }
}
