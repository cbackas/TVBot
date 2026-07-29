import {
  type APIApplicationCommandAutocompleteInteraction,
  type APIApplicationCommandInteraction,
  type APIApplicationCommandInteractionDataOption,
  type APIMessageComponentInteraction,
  ApplicationCommandOptionType,
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
