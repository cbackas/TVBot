import * as z from "npm:zod"

const envKeys = {
  "REGISTER_COMMANDS": z.stringbool().optional().default(true),
  "TZ": z.string().optional().default("America/Chicago"),
  "DISCORD_TOKEN": z.string(),
  "DISCORD_CLIENT_ID": z.string(),
  "DISCORD_GUILD_ID": z.string(),
  "UPDATE_SHOWS": z.stringbool().optional().default(true),
  "HEALTHCHECK_URL": z.string().optional(),
  "TVDB_API_KEY": z.string(),
  "TVDB_USER_PIN": z.string(),
  "NODE_ENV": z.enum(["development", "production"]).optional().default(
    "development",
  ),
  "DENO_KV_SQLITE_PATH": z.string().optional().default("./denokv.sqlite3"),
} as const
export type EnvKey = keyof typeof envKeys

/**
 * @throws Error if the environment variable doesn't match the zod schema
 */
export function getEnv<K extends EnvKey>(key: K): z.infer<(typeof envKeys)[K]> {
  const value = Deno.env.get(key)
  const parsed = parseEnv(key, value)
  return parsed !== undefined ? parsed : undefined
}

/**
 * @throws Error if the environment variable is not valid
 */
function parseEnv(key: EnvKey, value: unknown) {
  const parsedValue = envKeys[key].safeParse(value)
  if (parsedValue.error != null) {
    throw new Error(
      `Environment variable ${key} is not valid: ${parsedValue.error.toString()}`,
    )
  }
  return parsedValue.data
}
