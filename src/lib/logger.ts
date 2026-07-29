import { AsyncLocalStorage } from "node:async_hooks";

/**
 * Per-execution structured logging.
 *
 * We override the global `console` methods so any `console.info("hi")` call
 * anywhere downstream emits a single structured object — the readable message
 * plus whatever context has been pinned for the current execution (interaction
 * id, command name, user id, …). Cloudflare Workers Logs indexes those fields
 * into queryable columns; `wrangler tail` prints the object.
 *
 * Context is carried with AsyncLocalStorage (the Workers equivalent of Node
 * request-context), so you never have to thread a logger or ids through call
 * signatures — just `console.x(...)` and the context rides along. Requires the
 * `nodejs_als` compatibility flag (see wrangler.jsonc).
 */

export type LogContext = Record<string, unknown>;

const store = new AsyncLocalStorage<LogContext>();

// Capture the real console methods once, before we replace them below, so the
// wrappers can delegate to them without recursing into themselves.
const native = {
  debug: console.debug.bind(console),
  info: console.info.bind(console),
  warn: console.warn.bind(console),
  error: console.error.bind(console),
  log: console.log.bind(console),
};

type Level = keyof typeof native;

function serializeError(error: Error): Record<string, unknown> {
  return { name: error.name, message: error.message, stack: error.stack };
}

function emit(level: Level, args: unknown[]): void {
  const messages: string[] = [];
  const fields: LogContext = {};

  // Leading strings form the human message; objects merge into fields; Errors
  // get serialized under `error`. Explicit per-call fields win over context.
  for (const arg of args) {
    if (typeof arg === "string") {
      messages.push(arg);
    } else if (arg instanceof Error) {
      fields.error = serializeError(arg);
    } else if (arg != null && typeof arg === "object") {
      Object.assign(fields, arg);
    } else {
      messages.push(String(arg));
    }
  }

  native[level]({
    message: messages.join(" "),
    ...store.getStore(),
    ...fields,
  });
}

console.debug = (...args) => emit("debug", args);
console.info = (...args) => emit("info", args);
console.warn = (...args) => emit("warn", args);
console.error = (...args) => emit("error", args);
console.log = (...args) => emit("log", args);

/**
 * Run `fn` with a fresh log context. Every `console.*` call made anywhere
 * within `fn` — including across awaits — merges these fields into its output.
 */
export function runWithLogContext<T>(context: LogContext, fn: () => T): T {
  return store.run({ ...context }, fn);
}

/**
 * Pin extra fields onto the active log context so every later log in this
 * execution carries them. No-op outside a `runWithLogContext` scope.
 */
export function addLogContext(fields: LogContext): void {
  const current = store.getStore();
  if (current) Object.assign(current, fields);
}
