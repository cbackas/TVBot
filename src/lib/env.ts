import { env } from "cloudflare:workers";
import * as z from "zod";

const envKeys = {
  REGISTER_COMMANDS: z.stringbool().optional().default(true),
  TZ: z.string().optional().default("America/Chicago"),
  DISCORD_TOKEN: z.string(),
  DISCORD_CLIENT_ID: z.string(),
  DISCORD_GUILD_ID: z.string(),
  UPDATE_SHOWS: z.stringbool().optional().default(true),
  HEALTHCHECK_URL: z.string().optional(),
  TVDB_API_KEY: z.string(),
  TVDB_USER_PIN: z.string(),
  NODE_ENV: z
    .enum(["development", "production"])
    .optional()
    .default("development"),
} as const;
export type EnvKey = keyof typeof envKeys;

/**
 * @throws Error if the environment variable doesn't match the zod schema
 */
export function getEnv<K extends EnvKey>(key: K): z.infer<(typeof envKeys)[K]> {
  const parsed = envKeys[key].safeParse(env[key]);
  if (parsed.error != null) {
    throw new Error(
      `Environment variable ${key} is not valid: ${parsed.error.toString()}`,
    );
  }
  // Cast is safe: safeParse guarantees data matches the schema for key K
  return parsed.data as z.infer<(typeof envKeys)[K]>;
}
