import { settingsDB } from "lib/database/clients.ts"

export async function createSettingsTable() {
  await settingsDB.migrate([
    "CREATE TABLE settings (id INTEGER PRIMARY KEY, key TEXT UNIQUE NOT NULL, value TEXT NOT NULL)",
  ])
}
