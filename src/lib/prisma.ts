import process from "node:process"
import { PrismaClient } from "prisma-client/client.ts"

declare global {
  // deno-lint-ignore no-var
  var prisma: PrismaClient | undefined
}

export const DBChannelType = {
  FORUM: "FORUM",
  TEXT: "TEXT",
} as const

console.log(process.env.NODE_ENV)

const client = globalThis.prisma ?? new PrismaClient()
if (process.env.NODE_ENV !== "production") globalThis.prisma = client

export default client
