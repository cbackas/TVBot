import "jsr:@std/dotenv/load"
import {
  ChannelType,
  Client,
  ClientUser,
  Events,
  GatewayIntentBits,
} from "npm:discord.js"
import { CommandManager } from "lib/commandManager.ts"
import {
  checkForAiringEpisodes,
  pruneUnsubscribedShows,
  removeAllSubscriptions,
} from "lib/shows.ts"
import { sendAiringMessages } from "lib/episodeNotifier.ts"
import { Settings } from "lib/settingsManager.ts"
import {
  setRandomShowActivity,
  setTVDBLoadingActivity,
} from "lib/discordActivities.ts"
import { getEnv } from "lib/env.ts"
import { scheduleCronJobs } from "./cron.ts"
import assert from "node:assert"

const token = getEnv("DISCORD_TOKEN")
const clientId = getEnv("DISCORD_CLIENT_ID")
const guildId = getEnv("DISCORD_GUILD_ID")

const discordClient = new Client({ intents: [GatewayIntentBits.Guilds] })

const commandManager = new CommandManager(clientId, token, guildId)

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

/**
 * When a thread (forum post) is deleted, remove all subscriptions for that post
 */
discordClient.on(Events.ThreadDelete, async (thread) => {
  await removeAllSubscriptions(thread.id, "channelId")
  await pruneUnsubscribedShows()
})

/**
 * When a forum is deleted, remove all subscriptions for post in that forum
 */
discordClient.on(Events.ChannelDelete, async (channel) => {
  if (channel.type === ChannelType.GuildForum) {
    await removeAllSubscriptions(channel.id, "forumId")
    await pruneUnsubscribedShows()
  }

  if (channel.type === ChannelType.GuildText) {
    await removeAllSubscriptions(channel.id, "channelId")
    await pruneUnsubscribedShows()
    await Settings.removeGlobalDestination(channel.id)
  }
})

// start the bot
await Settings.refresh()
await commandManager.registerCommands()
await discordClient.login(token)

export const getClient = (): Client<boolean> => discordClient
export const getClientUser = (): ClientUser => {
  assert(discordClient.user != null, "Client user is null")
  return discordClient.user
}
