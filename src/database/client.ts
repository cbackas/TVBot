import { getEnv } from "lib/env.ts"

const dbLocation = getEnv("DENO_KV_SQLITE_PATH")
const kv = await Deno.openKv(dbLocation)

export default kv
