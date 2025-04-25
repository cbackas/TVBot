import { dataDB, settingsDB } from "lib/database/clients.ts"

export async function runMigrations() {
  // settings table
  await settingsDB.migrate([
    "CREATE TABLE settings (id INTEGER PRIMARY KEY, key TEXT UNIQUE NOT NULL, value TEXT NOT NULL)",
  ])

  // data table
  await dataDB.migrate([])
}
