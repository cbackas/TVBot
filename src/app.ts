import "jsr:@std/dotenv/load"
import { Client, ClientUser, Events, GatewayIntentBits } from "npm:discord.js"
import { CommandManager } from "lib/commandManager.ts"
import { checkForAiringEpisodes, pruneUnsubscribedShows } from "lib/shows.ts"
import { sendAiringMessages } from "lib/episodeNotifier.ts"
import {
  setRandomShowActivity,
  setTVDBLoadingActivity,
} from "lib/discordActivities.ts"
import { getEnv } from "lib/env.ts"
import { scheduleCronJobs } from "./cron.ts"
import assert from "node:assert"
import { handleChannelDelete, handleThreadDelete } from "./handlers.ts"

const token = getEnv("DISCORD_TOKEN")
const clientId = getEnv("DISCORD_CLIENT_ID")
const guildId = getEnv("DISCORD_GUILD_ID")

const commandManager = new CommandManager()
await commandManager.registerCommands(clientId, token, guildId)

const discordClient = new Client({ intents: [GatewayIntentBits.Guilds] })

discordClient.on(Events.ClientReady, async (client) => {
  if (client.user == null) {
    throw new Error("Fatal: Client user is null")
  }
  console.info(`Logged in as ${client.user.tag}!`)

  // run initial scheduled activities
  setTVDBLoadingActivity()
  await pruneUnsubscribedShows()
  if (getEnv("UPDATE_SHOWS")) await checkForAiringEpisodes()
  void sendAiringMessages()
  void setRandomShowActivity()

  scheduleCronJobs()
})

discordClient.on(Events.InteractionCreate, commandManager.interactionHandler)
discordClient.on(Events.ThreadDelete, handleThreadDelete)
discordClient.on(Events.ChannelDelete, handleChannelDelete)

// start the bot
await discordClient.login(token)

export const getClient = (): Client<boolean> => discordClient
export const getClientUser = (): ClientUser => {
  assert(discordClient.user != null, "Client user is null")
  return discordClient.user
}
