import * as dotenv from 'dotenv'
import schedule from 'node-schedule'
import { ActivityType, ChannelType, Client, Events, GatewayIntentBits } from 'discord.js'
import { scheduleAiringMessages } from './lib/episodeNotifier'
import { CommandManager } from './lib/commandManager'
import { checkForAiringEpisodes, pruneUnsubscribedShows, removeAllSubscriptions } from './lib/shows'
import { SettingsManager } from './lib/settingsManager'
import { sendMorningSummary } from './lib/morningSummary'

dotenv.config()

/**
 * The main bot application
 */
export class App {
  private client: Client
  private commands: CommandManager
  private settings: SettingsManager

  private token: string
  private clientId: string
  private guildId: string

  constructor() {
    if (process.env.DISCORD_TOKEN === undefined) throw new Error('DISCORD_TOKEN is not defined')
    if (process.env.DISCORD_CLIENT_ID === undefined) throw new Error('DISCORD_CLIENT_ID is not defined')
    if (process.env.DISCORD_GUILD_ID === undefined) throw new Error('DISCORD_GUILD_ID is not defined')
    if (process.env.TZ === undefined) throw new Error('TZ is not defined')

    this.token = process.env.DISCORD_TOKEN
    this.clientId = process.env.DISCORD_CLIENT_ID
    this.guildId = process.env.DISCORD_GUILD_ID

    this.client = new Client({ intents: [GatewayIntentBits.Guilds] })
    this.commands = new CommandManager(this, this.clientId, this.token, this.guildId)
    this.settings = new SettingsManager()

    this.init()
  }

  /**
   * Async init function for app
   */
  private init = async (): Promise<void> => {
    await this.settings.refresh()
    await this.commands.registerCommands()
    this.startBot()
  }

  /**
   * Start the bot and register listeners
   */
  private startBot = (): void => {
    this.client.on(Events.ClientReady, async () => {
      const { user } = this.client
      if (user !== null) console.log(`Logged in as ${user.tag}!`)

      // run initial scheduled activities
      this.randomWatchingActivity()
      if (process.env.UPDATE_SHOWS !== 'false') await checkForAiringEpisodes()
      scheduleAiringMessages(this)

      schedule.scheduleJob('lifecycle:1hour:updateBotActivity', '15 * * * *', () => {
        this.randomWatchingActivity()
      })

      schedule.scheduleJob('lifecycle:4hours:fetchEpisoded', '0 */4 * * *', async () => {
        await checkForAiringEpisodes()
        scheduleAiringMessages(this)
      })

      schedule.scheduleJob('lifecycle:morningSummary', '0 8 * * *', async () => {
        const settings = this.getSettings()
        if (!settings) throw new Error('Settings not found')

        await sendMorningSummary(settings, this.client)
      })
    })

    this.client.on(Events.InteractionCreate, this.commands.interactionHandler)

    /**
     * When a thread (forum post) is deleted, remove all subscriptions for that post
     */
    this.client.on(Events.ThreadDelete, async (thread) => {
      await removeAllSubscriptions(thread.id, 'channelId')
      await pruneUnsubscribedShows()
    })

    /**
     * When a forum is deleted, remove all subscriptions for post in that forum
     */
    this.client.on(Events.ChannelDelete, async (channel) => {
      if (channel.type === ChannelType.GuildForum) {
        await removeAllSubscriptions(channel.id, 'forumId')
        await pruneUnsubscribedShows()
      }

      if (channel.type === ChannelType.GuildText) {
        await removeAllSubscriptions(channel.id, 'channelId')
        await pruneUnsubscribedShows()
        await this.settings.removeGlobalDestination(channel.id)
      }
    })

    void this.client.login(this.token)
  }

  private randomWatchingActivity = (): void => {
    const showList: { shows: string[] } = require('../assets/shows.json')
    const randomIndex = Math.floor(Math.random() * (showList.shows.length - 0 + 1) + 0)
    this.client.user?.setActivity(showList.shows[randomIndex], { type: ActivityType.Watching })
  }

  public getClient = () => this.client
  public getSettings = () => this.settings.fetch()
  public getSettingsManager = () => this.settings
}

// make an instance of the application class
new App()
