/**
 * Thin structured-logging wrapper over `console`.
 *
 * Cloudflare Workers Logs (observability) indexes the fields of an object
 * passed to a `console` method, turning them into queryable columns in the
 * dashboard — while `wrangler tail` still prints the human-readable message.
 * So every log carries a readable `message` plus an optional `fields` object
 * of structured metadata (commandName, userId, guildId, …).
 */

export type LogFields = Record<string, unknown>;

type LogLevel = "debug" | "info" | "warn" | "error";

function emit(level: LogLevel, message: string, fields?: LogFields): void {
  if (fields && Object.keys(fields).length > 0) {
    console[level](message, fields);
  } else {
    console[level](message);
  }
}

export const logger = {
  debug: (message: string, fields?: LogFields) =>
    emit("debug", message, fields),
  info: (message: string, fields?: LogFields) => emit("info", message, fields),
  warn: (message: string, fields?: LogFields) => emit("warn", message, fields),
  error: (message: string, fields?: LogFields) =>
    emit("error", message, fields),
};
