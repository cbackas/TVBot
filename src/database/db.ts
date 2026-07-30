import { env } from "cloudflare:workers";
import { drizzle } from "drizzle-orm/d1";
import { relations } from "./relations.js";
import * as schema from "./schema.js";

export function getDb() {
  return drizzle(env.DB, { schema, relations });
}
