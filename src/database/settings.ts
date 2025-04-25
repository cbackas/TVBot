import * as z from "npm:zod"
import { zDestination } from "../schemas.ts"
import kv from "./client.ts"

const settingSchema = {
  defaultForum: z.string().nullable(),
  allEpisodes: z.array(zDestination),
  morningSumarryDestinations: z.array(zDestination),
} as const
export type SettingKey = keyof typeof settingSchema

export async function getSetting<K extends SettingKey>(
  key: K,
): Promise<z.infer<(typeof settingSchema)[K]>> {
  const kvEntry = await kv.get(["settings", key])
  const parsed: z.infer<(typeof settingSchema)[K]> = parseValue(
    key,
    kvEntry.value,
  )
  return parsed
}

export async function setSetting<K extends SettingKey>(
  key: K,
  value: z.infer<(typeof settingSchema)[K]>,
): Promise<Deno.KvCommitResult> {
  const parsedValue = parseValue(key, value)
  return await kv.set(["settings", key], parsedValue)
}

/**
 * @throws Error if the setting variable is not valid
 */
function parseValue(key: SettingKey, value: unknown) {
  const parsedValue = settingSchema[key].safeParse(value)
  if (parsedValue.error != null) {
    throw new Error(
      `Setting ${key} is not valid: ${parsedValue.error.toString()}`,
    )
  }
  return parsedValue.data
}
