import { PrismaClient } from "prisma-client/client.ts"
import { getEnv } from "lib/env.ts"

declare global {
  var prisma: PrismaClient | undefined
}

export const DBChannelType = {
  FORUM: "FORUM",
  TEXT: "TEXT",
} as const

const client = globalThis.prisma ?? new PrismaClient()
if (getEnv("NODE_ENV") !== "production") globalThis.prisma = client

export default client
