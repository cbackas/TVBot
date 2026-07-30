import {
  type APIApplicationCommandAutocompleteInteraction,
  type APIApplicationCommandInteraction,
  type APIApplicationCommandInteractionDataOption,
  type APIApplicationCommandOption,
  type APIMessageComponentInteraction,
  ApplicationCommandOptionType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";

type AnyCommandInteraction =
  | APIApplicationCommandInteraction
  | APIApplicationCommandAutocompleteInteraction;

/**
 * The guild an interaction came from. All of our commands are Guild-context
 * only (`contexts: [InteractionContextType.Guild]`), so this is always present
 * in practice — but Discord types it optional, so callers must handle null.
 */
export function getGuildId(
  interaction:
    | APIApplicationCommandInteraction
    | APIMessageComponentInteraction,
): string | null {
  return interaction.guild_id ?? null;
}

/**
 * Render the invoking user as `id (username)`. IDs are stable and safe to
 * grep/correlate on; usernames are for human eyeballs only and can contain
 * spaces or unicode — so the ID leads and the name is the parenthetical.
 */
export function formatUser(
  interaction:
    | APIApplicationCommandInteraction
    | APIMessageComponentInteraction,
): string {
  const user = interaction.member?.user ?? interaction.user;
  if (!user) return "unknown user";
  return `${user.id} (${user.username})`;
}

function getOptions(
  interaction: AnyCommandInteraction,
): APIApplicationCommandInteractionDataOption[] {
  if ("options" in interaction.data && interaction.data.options) {
    return interaction.data.options;
  }
  return [];
}

/**
 * Unwrap subcommand group → subcommand → options from a raw interaction.
 * Returns the flat options array at the deepest level.
 */
function resolveOptions(
  interaction: AnyCommandInteraction,
): APIApplicationCommandInteractionDataOption[] {
  let options = getOptions(interaction);

  // Unwrap subcommand group
  if (
    options.length === 1 &&
    options[0].type === ApplicationCommandOptionType.SubcommandGroup
  ) {
    options = options[0].options ?? [];
  }

  // Unwrap subcommand
  if (
    options.length === 1 &&
    options[0].type === ApplicationCommandOptionType.Subcommand
  ) {
    options = options[0].options ?? [];
  }

  return options;
}

export function getSubcommandGroup(
  interaction: AnyCommandInteraction,
): string | null {
  const options = getOptions(interaction);
  if (
    options.length >= 1 &&
    options[0].type === ApplicationCommandOptionType.SubcommandGroup
  ) {
    return options[0].name;
  }
  return null;
}

export function getSubcommand(
  interaction: AnyCommandInteraction,
): string | null {
  let options = getOptions(interaction);

  // Unwrap subcommand group first
  if (
    options.length >= 1 &&
    options[0].type === ApplicationCommandOptionType.SubcommandGroup
  ) {
    options = options[0].options ?? [];
  }

  if (
    options.length >= 1 &&
    options[0].type === ApplicationCommandOptionType.Subcommand
  ) {
    return options[0].name;
  }
  return null;
}

/**
 * Walk a command definition and collect, per option name, a map of choice
 * value → human label. Lets logs show `task:Refresh episode data` instead of
 * the raw `task:check_episodes` the user actually sent.
 */
function collectChoiceLabels(
  options: APIApplicationCommandOption[] | undefined,
  acc: Map<string, Map<string, string>>,
): void {
  for (const opt of options ?? []) {
    if ("choices" in opt && opt.choices) {
      const labels = acc.get(opt.name) ?? new Map<string, string>();
      for (const choice of opt.choices) {
        labels.set(String(choice.value), choice.name);
      }
      acc.set(opt.name, labels);
    }
    if ("options" in opt && opt.options) {
      collectChoiceLabels(opt.options, acc);
    }
  }
}

/**
 * Render an interaction the way the user typed it, e.g.
 * `/trigger task:Refresh episode data` or `/setting morning_summary add
 * channel:1054…`. When the command `definition` is provided, choice values are
 * resolved to their display labels.
 */
export function formatCommandInvocation(
  interaction: APIApplicationCommandInteraction,
  definition?: RESTPostAPIChatInputApplicationCommandsJSONBody,
): string {
  const parts: string[] = [`/${interaction.data.name}`];

  const group = getSubcommandGroup(interaction);
  if (group) parts.push(group);
  const sub = getSubcommand(interaction);
  if (sub) parts.push(sub);

  const choiceLabels = new Map<string, Map<string, string>>();
  if (definition) collectChoiceLabels(definition.options, choiceLabels);

  for (const opt of resolveOptions(interaction)) {
    if (!("value" in opt)) continue;
    const raw = String(opt.value);
    const label = choiceLabels.get(opt.name)?.get(raw) ?? raw;
    parts.push(`${opt.name}:${label}`);
  }

  return parts.join(" ");
}

export function getStringOption(
  interaction: AnyCommandInteraction,
  name: string,
): string | null {
  const options = resolveOptions(interaction);
  const opt = options.find(
    (o) => o.name === name && o.type === ApplicationCommandOptionType.String,
  );
  return opt && "value" in opt ? String(opt.value) : null;
}

export function getChannelOption(
  interaction: AnyCommandInteraction,
  name: string,
): string | null {
  const options = resolveOptions(interaction);
  const opt = options.find(
    (o) => o.name === name && o.type === ApplicationCommandOptionType.Channel,
  );
  return opt && "value" in opt ? String(opt.value) : null;
}

export interface ResolvedChannelInfo {
  id: string;
  type: number;
  name: string | null;
}

export function getResolvedChannel(
  interaction: APIApplicationCommandInteraction,
  name: string,
): ResolvedChannelInfo | null {
  const id = getChannelOption(interaction, name);
  if (id == null) return null;
  if (!("resolved" in interaction.data) || interaction.data.resolved == null) {
    return null;
  }
  const resolved = interaction.data.resolved;
  if (!("channels" in resolved) || resolved.channels == null) return null;
  const channel = resolved.channels[id];
  if (channel == null) return null;
  return { id, type: channel.type, name: channel.name ?? null };
}
