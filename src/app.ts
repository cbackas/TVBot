import "jsr:@std/dotenv/load"
import process from "node:process"
import { ChannelType, Client, Events, GatewayIntentBits } from "npm:discord.js"
import { CommandManager } from "lib/commandManager.ts"
import {
  checkForAiringEpisodes,
  pruneUnsubscribedShows,
  removeAllSubscriptions,
} from "lib/shows.ts"
import { sendAiringMessages } from "lib/episodeNotifier.ts"
import { Settings } from "lib/settingsManager.ts"
import { sendMorningSummary } from "lib/morningSummary.ts"
import {
  setRandomShowActivity,
  setTVDBLoadingActivity,
} from "lib/discordActivities.ts"

if (process.env.DISCORD_TOKEN === undefined) {
  throw new Error("DISCORD_TOKEN is not defined")
}
if (process.env.DISCORD_CLIENT_ID === undefined) {
  throw new Error("DISCORD_CLIENT_ID is not defined")
}
if (process.env.DISCORD_GUILD_ID === undefined) {
  throw new Error("DISCORD_GUILD_ID is not defined")
}
if (process.env.TZ === undefined) throw new Error("TZ is not defined")

const token = process.env.DISCORD_TOKEN
const clientId = process.env.DISCORD_CLIENT_ID
const guildId = process.env.DISCORD_GUILD_ID

const discordClient = new Client({ intents: [GatewayIntentBits.Guilds] })

const commandManager = new CommandManager(clientId, token, guildId)
const settingsManager = Settings.getInstance()

discordClient.on(Events.ClientReady, async (client) => {
  const { user } = client
  if (user == null) throw new Error("User is null")
  console.log(`Logged in as ${user.tag}!`)

  // run initial scheduled activities
  setTVDBLoadingActivity(user)
  await pruneUnsubscribedShows()
  if (process.env.UPDATE_SHOWS !== "false") await checkForAiringEpisodes()
  void sendAiringMessages()
  void setRandomShowActivity(user)

  Deno.cron("Announcements", { minute: { every: 10, start: 8 } }, () => {
    void sendAiringMessages()
    void setRandomShowActivity(user)
  })

  Deno.cron("Fetch Episode Data", { hour: { every: 4 } }, async () => {
    setTVDBLoadingActivity(user)
    await pruneUnsubscribedShows()
    await checkForAiringEpisodes()
  })

  Deno.cron("Morning Summary", { hour: 8, minute: 0 }, async () => {
    const settings = settingsManager.fetch()
    if (settings == null) throw new Error("Settings not found")

    await sendMorningSummary(settings, client)
  })

  const healthcheckUrl = process.env.HEALTHCHECK_URL
  if (healthcheckUrl != null) {
    Deno.cron("Healthcheck", { minute: { every: 1 } }, async () => {
      await fetch(healthcheckUrl)
      console.debug("[Healthcheck] Healthcheck ping sent")
    })
  }
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
    await settingsManager.removeGlobalDestination(channel.id)
  }
})

// start the bot
await settingsManager.refresh()
await commandManager.registerCommands()
await discordClient.login(token)

export const getClient = (): Client<boolean> => discordClient
