import { getEnv } from "lib/env.ts"

const useLocal: boolean = getEnv("DENO_KV_ACCESS_TOKEN") === undefined
const kv = await Deno.openKv(
  useLocal ? "./denokv.sqlite3" : "http://localhost:4512",
)

export default kv
