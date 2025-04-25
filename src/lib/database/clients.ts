import process from "node:process"
import { createClient } from "npm:@libsql/client"

const syncUrl = process.env.TURSO_DATABASE_URL
const authToken = process.env.TURSO_AUTH_TOKEN

if (syncUrl == null) {
  throw new Error("TURSO_DATABASE_URL is not set")
}
if (authToken == null) {
  throw new Error("TURSO_AUTH_TOKEN is not set")
}

export const settingsDB = createClient({
  url: "file://tvbot-settings.db",
  syncUrl: syncUrl,
  syncInterval: 500,
  authToken,
})

export const dataDB = createClient({
  url: "file://tvbot-data.db",
  syncUrl,
  syncInterval: 120,
  authToken,
})

export default { settingsDB, dataDB }
