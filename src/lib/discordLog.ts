import {
  ApplicationCommandOptionType,
  type ChatInputCommandInteraction,
  type CommandInteractionOption,
  DiscordAPIError,
  escapeMarkdown,
  type Client,
  type Message,
} from "discord.js"
import { parseIMDBIds } from "lib/util.ts"
import { Settings } from "lib/settingsManager.ts"

export const LOG_TYPES = [
  {
    key: "thread_ops",
    label: "Thread Ops",
    description: "/post, /link, /unlink and show thread changes",
  },
  {
    key: "tasks",
    label: "Tasks",
    description: "Scheduled jobs that complete",
  },
  {
    key: "task_errors",
    label: "Task Errors",
    description: "Scheduled jobs and notifications that fail",
  },
  {
    key: "api_errors",
    label: "API Errors",
    description: "Failed Discord and TVDB API requests",
  },
  {
    key: "command_errors",
    label: "Command Errors",
    description: "Commands, menus and autocompletes that crash",
  },
  {
    key: "other",
    label: "Other",
    description: "Anything else",
  },
] as const

export type LogType = typeof LOG_TYPES[number]["key"]

const MAX_MESSAGE_LENGTH = 2000
const MAX_ERROR_LENGTH = 1900
const MAX_DETAIL_LENGTH = 300
const REPEAT_WINDOW_MS = 5 * 60_000
const MAX_REMEMBERED_COMMANDS = 100

export interface ApiFailure {
  service: string
  url: string
  status: number
  body?: string
}

/**
 * Handle for finishing a logged command. Only the first call has an effect,
 * so a later failure can't overwrite an earlier result
 */
export interface CommandLog {
  /** replaces the logged command with `details` (default "Succeeded"), keeping who ran it and where */
  success: (details?: string) => Promise<void>
  /** appends text to the end of the logged command line */
  append: (text: string) => Promise<void>
  /** appends "(failed)" to the logged command line, with the reason if given */
  fail: (reason?: string) => Promise<void>
}

interface ErrorSummary {
  origin: string
  message: string
  status?: number
  code?: number | string
  endpoint?: string
}

interface LoggedError {
  message: Promise<Message | undefined>
  sentAt: number
  repeated: boolean
}

let discordClient: Client | undefined

/**
 * Gives the log module the client to send messages with. This is set from app.ts
 * instead of importing the client, because importing app.ts from here would create
 * an import cycle that deadlocks on the top level awaits in app.ts and commandManager.ts
 */
export function setLogClient(client: Client): void {
  discordClient = client
}

/**
 * Escapes markdown in text that is shown in a log message, including the
 * square brackets that would break a masked link
 */
export function escapeLogText(text: string): string {
  return escapeMarkdown(text).replace(/[[\]]/g, "\\$&")
}

/**
 * Wraps text in an inline code span. Backticks can't be escaped inside a code
 * span, so they are replaced with apostrophes
 */
export function codeSpan(text: string): string {
  return `\`${text.replaceAll("`", "'")}\``
}

const recentErrors = new Map<string, LoggedError>()
const commandLogs = new Map<string, CommandLog>()

function isLogTypeEnabled(type: LogType): boolean {
  return !(Settings.fetch()?.disabledLogTypes ?? []).includes(type)
}

async function sendLog(
  content: string,
  type: LogType,
): Promise<Message | undefined> {
  try {
    const channelId = Settings.fetch()?.logChannel
    if (channelId == null || !isLogTypeEnabled(type)) return undefined

    if (discordClient == null) return undefined

    const channel = await discordClient.channels.fetch(channelId)
    if (channel == null || !channel.isSendable()) {
      console.warn(`Log channel ${channelId} is missing or not sendable`)
      return undefined
    }

    return await channel.send({
      content: content.slice(0, MAX_MESSAGE_LENGTH),
      allowedMentions: { parse: [] },
    })
  } catch (error) {
    console.warn("Failed to send to the log channel:", error)
  }
  return undefined
}

async function editLog(
  message: Message | undefined,
  content: string,
): Promise<void> {
  if (message == null) return

  try {
    await message.edit({
      content: content.slice(0, MAX_MESSAGE_LENGTH),
      allowedMentions: { parse: [] },
    })
  } catch (error) {
    console.warn("Failed to edit the log message:", error)
  }
}

function describeError(error: unknown, origin?: string): ErrorSummary {
  if (error instanceof DiscordAPIError) {
    return {
      origin: "Discord API",
      message: error.message,
      status: error.status,
      code: error.code,
      endpoint: `${error.method} ${error.url}`,
    }
  }
  if (error instanceof Error) {
    return {
      origin: origin != null ? `${origin} (${error.name})` : error.name,
      message: error.message,
    }
  }
  return { origin: origin ?? "Unknown error", message: String(error) }
}

function describeApiFailure(failure: ApiFailure): ErrorSummary {
  return {
    origin: failure.service,
    message: failure.body?.slice(0, MAX_DETAIL_LENGTH) ?? "Request failed",
    status: failure.status,
    endpoint: failure.url,
  }
}

function formatError(context: string, summary: ErrorSummary): string {
  const details = [
    summary.origin,
    summary.status != null ? `HTTP ${summary.status}` : undefined,
    summary.code != null ? `code ${summary.code}` : undefined,
  ].filter((part) => part != null).join(" · ")

  const lines = [
    `⚠️ **${escapeLogText(context)}**`,
    `${details}: ${escapeLogText(summary.message)}`,
  ]
  if (summary.endpoint != null) lines.push(`\`${summary.endpoint}\``)

  return lines.join("\n").slice(0, MAX_ERROR_LENGTH)
}

/**
 * Sends an error to the log channel. If the same error was already sent recently,
 * the original message is edited once to say it repeated, and further repeats are ignored
 */
async function logError(content: string, type: LogType): Promise<void> {
  if (Settings.fetch()?.logChannel == null || !isLogTypeEnabled(type)) return

  const now = Date.now()
  for (const [key, entry] of recentErrors) {
    if (now - entry.sentAt > REPEAT_WINDOW_MS) recentErrors.delete(key)
  }

  const existing = recentErrors.get(content)
  if (existing == null) {
    recentErrors.set(content, {
      message: sendLog(content, type),
      sentAt: now,
      repeated: false,
    })
    return
  }

  if (existing.repeated) return
  existing.repeated = true
  await editLog(await existing.message, `${content}\n(repeated)`)
}

/**
 * Logs an error to the console and to the configured log channel (if any)
 * @param context short description of what was being attempted
 * @param error the thrown value
 * @param options.origin optional label for where the error came from, shown instead of the bare error class
 * @param options.type log type that controls whether this is posted, Discord API errors are always `api_errors`
 */
export function reportError(
  context: string,
  error: unknown,
  options?: { origin?: string; type?: LogType },
): void {
  console.error(`${context}:`, error)
  const type = error instanceof DiscordAPIError
    ? "api_errors"
    : (options?.type ?? "other")
  void logError(
    formatError(context, describeError(error, options?.origin)),
    type,
  )
}

/**
 * Logs a failed API response to the console and to the configured log channel (if any)
 * @param context short description of what was being attempted
 * @param failure details of the failed response
 */
export function reportApiFailure(context: string, failure: ApiFailure): void {
  console.error(`${context}:`, failure)
  void logError(
    formatError(context, describeApiFailure(failure)),
    "api_errors",
  )
}

/**
 * Posts a plain message to the log channel (if one is set)
 * @param content message to post
 * @param type log type that controls whether this is posted
 */
export function logMessage(content: string, type: LogType = "other"): void {
  void sendLog(content, type)
}

function formatOptions(options: readonly CommandInteractionOption[]): string {
  return options.map((option) => {
    switch (option.type) {
      case ApplicationCommandOptionType.Subcommand:
      case ApplicationCommandOptionType.SubcommandGroup:
        return `${option.name} ${formatOptions(option.options ?? [])}`.trim()
      case ApplicationCommandOptionType.Channel:
        return `${option.name}:<#${option.value}>`
      case ApplicationCommandOptionType.User:
        return `${option.name}:<@${option.value}>`
      case ApplicationCommandOptionType.Role:
        return `${option.name}:<@&${option.value}>`
      default:
        // show IMDB ids rather than whatever url or list was typed,
        // unless none could be read from it
        if (option.name === "imdb_id" && typeof option.value === "string") {
          const ids = parseIMDBIds(option.value)
          if (ids.length > 0) return `${option.name}:${ids.join(",")}`
        }
        return `${option.name}:${escapeLogText(String(option.value))}`
    }
  }).join(" ")
}

function formatCommand(interaction: ChatInputCommandInteraction): string {
  return `/${interaction.commandName} ${formatOptions(interaction.options.data)}`
    .replace(/\s+/g, " ")
    .trim()
}

/**
 * Logs that a command was used, including who used it, where and with which arguments.
 * The log message is sent in the background so the command doesn't wait for it
 * @param interaction the command interaction to log
 * @returns handle that edits the log message when the command finishes
 */
export function logCommandUse(
  interaction: ChatInputCommandInteraction,
): CommandLog {
  const who = `(<@${interaction.user.id}> in <#${interaction.channelId}>)`
  const content = `> ${formatCommand(interaction)} ${who}`

  const message = sendLog(content, "thread_ops")
  let finished = false

  // edits the log message, unless an earlier call already did
  const finish = async (newContent: string): Promise<void> => {
    if (finished) return
    finished = true
    await editLog(await message, newContent)
  }

  return {
    success: async (details?: string) => {
      const result = details != null && details !== "" ? details : "Succeeded"
      // leave out the channel when the result already mentions it
      const suffix = result.includes(`<#${interaction.channelId}>`)
        ? `(<@${interaction.user.id}>)`
        : who
      await finish(`${result} ${suffix}`)
    },
    append: async (text: string) => {
      await finish(`${content} ${text}`)
    },
    fail: async (reason?: string) => {
      const text = reason != null && reason !== ""
        ? `(failed: ${reason})`
        : "(failed)"
      await finish(`${content} ${text}`)
    },
  }
}

/**
 * Remember a command log for a reply message, for commands that finish in a later interaction
 * @param messageId id of the reply message that the later interaction will come from
 * @param log the log handle returned from `logCommandUse`
 */
export function rememberCommandLog(messageId: string, log: CommandLog): void {
  if (commandLogs.size >= MAX_REMEMBERED_COMMANDS) {
    const oldest = commandLogs.keys().next().value
    if (oldest != null) commandLogs.delete(oldest)
  }
  commandLogs.set(messageId, log)
}

/**
 * Retrieve (and forget) a command log remembered with `rememberCommandLog`
 * @param messageId id of the reply message
 */
export function takeCommandLog(messageId: string): CommandLog | undefined {
  const log = commandLogs.get(messageId)
  commandLogs.delete(messageId)
  return log
}
