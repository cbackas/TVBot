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
import { type Settings, SettingsManager } from "lib/settingsManager.ts"
import { sendMorningSummary } from "lib/morningSummary.ts"
import {
  setRandomShowActivity,
  setTVDBLoadingActivity,
} from "lib/discordActivities.ts"

/**
 * The main bot application
 */
export class App {
  private readonly client: Client
  private readonly commands: CommandManager
  private readonly settings: SettingsManager

  private readonly token: string
  private readonly clientId: string
  private readonly guildId: string

  constructor() {
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

    this.token = process.env.DISCORD_TOKEN
    this.clientId = process.env.DISCORD_CLIENT_ID
    this.guildId = process.env.DISCORD_GUILD_ID

    this.client = new Client({ intents: [GatewayIntentBits.Guilds] })
    this.commands = new CommandManager(
      this,
      this.clientId,
      this.token,
      this.guildId,
    )
    this.settings = new SettingsManager()

    void this.init()
  }

  /**
   * Async init function for app
   */
  private readonly init = async (): Promise<void> => {
    await this.settings.refresh()
    await this.commands.registerCommands()
    this.startBot()
  }

  /**
   * Start the bot and register listeners
   */
  private readonly startBot = (): void => {
    this.client.on(Events.ClientReady, async () => {
      const { user } = this.client
      if (user == null) throw new Error("User is null")
      console.log(`Logged in as ${user.tag}!`)

      // run initial scheduled activities
      setTVDBLoadingActivity(user)
      await pruneUnsubscribedShows()
      if (process.env.UPDATE_SHOWS !== "false") await checkForAiringEpisodes()
      void sendAiringMessages(this)
      void setRandomShowActivity(user)

      Deno.cron("Announcements", { minute: { every: 10, start: 8 } }, () => {
        void sendAiringMessages(this)
        void setRandomShowActivity(user)
      })

      Deno.cron("Fetch Episode Data", { hour: { every: 4 } }, async () => {
        setTVDBLoadingActivity(user)
        await pruneUnsubscribedShows()
        await checkForAiringEpisodes()
      })

      Deno.cron("Morning Summary", { hour: 8, minute: 0 }, async () => {
        const settings = this.getSettings()
        if (settings == null) throw new Error("Settings not found")

        await sendMorningSummary(settings, this.client)
      })

      const healthcheckUrl = process.env.HEALTHCHECK_URL
      if (healthcheckUrl != null) {
        Deno.cron("Healthcheck", { minute: { every: 1 } }, async () => {
          await fetch(healthcheckUrl)
          console.debug("[Healthcheck] Healthcheck ping sent")
        })
      }
    })

    this.client.on(Events.InteractionCreate, this.commands.interactionHandler)

    /**
     * When a thread (forum post) is deleted, remove all subscriptions for that post
     */
    this.client.on(Events.ThreadDelete, async (thread) => {
      await removeAllSubscriptions(thread.id, "channelId")
      await pruneUnsubscribedShows()
    })

    /**
     * When a forum is deleted, remove all subscriptions for post in that forum
     */
    this.client.on(Events.ChannelDelete, async (channel) => {
      if (channel.type === ChannelType.GuildForum) {
        await removeAllSubscriptions(channel.id, "forumId")
        await pruneUnsubscribedShows()
      }

      if (channel.type === ChannelType.GuildText) {
        await removeAllSubscriptions(channel.id, "channelId")
        await pruneUnsubscribedShows()
        await this.settings.removeGlobalDestination(channel.id)
      }
    })

    void this.client.login(this.token)
  }

  public getClient = (): Client<boolean> => this.client
  public getSettings = (): Settings | undefined => this.settings.fetch()
  public getSettingsManager = (): SettingsManager => this.settings
} // make an instance of the application class

;(() => new App())()
