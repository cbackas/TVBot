import type {
  APIApplicationCommandAutocompleteInteraction,
  APIApplicationCommandInteraction,
  APIMessageComponentInteraction,
} from "discord-api-types/v10";

export interface Command {
  name: string;
  selectMenuIds?: string[];

  handler(interaction: APIApplicationCommandInteraction): Promise<Response>;
  autoComplete?(
    interaction: APIApplicationCommandAutocompleteInteraction,
  ): Promise<Response>;
  componentHandler?(
    interaction: APIMessageComponentInteraction,
  ): Promise<Response>;
}

export const commands: Map<string, Command> = new Map();

import LinkCommand from "./link.command.js";
import ListCommand from "./list.command.js";
import PingCommand from "./ping.command.js";
import PostCommand from "./post.command.js";
import SearchCommand from "./search.command.js";
import SettingCommand from "./setting.command.js";
import UnlinkCommand from "./unlink.command.js";
import UpcomingCommand from "./upcoming.command.js";

for (const cmd of [
  new PingCommand(),
  new SearchCommand(),
  new LinkCommand(),
  new ListCommand(),
  new UnlinkCommand(),
  new UpcomingCommand(),
  new SettingCommand(),
  new PostCommand(),
]) {
  commands.set(cmd.name, cmd);
}
