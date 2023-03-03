import * as dotenv from 'dotenv'
import schedule from 'node-schedule'
import { ActivityType, ChannelType, Client, Events, GatewayIntentBits } from 'discord.js'
import { scheduleAiringMessages } from './lib/episodeNotifier'
import { CommandManager } from './lib/commandManager'
import { checkForAiringEpisodes, pruneUnsubscribedShows, removeAllSubscriptions } from './lib/shows'
import { type Settings, SettingsManager } from './lib/settingsManager'
import { sendMorningSummary } from './lib/morningSummary'

dotenv.config()

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

  constructor () {
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
      if (user !== null) console.log(`Logged in as ${user.tag}!`)

      // run initial scheduled activities
      void this.randomWatchingActivity()
      if (process.env.UPDATE_SHOWS !== 'false') await checkForAiringEpisodes()
      void scheduleAiringMessages(this)

      schedule.scheduleJob('lifecycle:1hour:updateBotActivity', '15 * * * *', () => {
        void this.randomWatchingActivity()
      })

      schedule.scheduleJob('lifecycle:4hours:fetchEpisoded', '0 */4 * * *', async () => {
        await checkForAiringEpisodes()
        void scheduleAiringMessages(this)
      })

      schedule.scheduleJob('lifecycle:morningSummary', '0 8 * * *', async () => {
        const settings = this.getSettings()
        if (settings == null) throw new Error('Settings not found')

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

  private readonly randomWatchingActivity = async (): Promise<void> => {
    const showList: { shows: string[] } = await import('../assets/shows.json')
    const randomIndex = Math.floor(Math.random() * (showList.shows.length - 0 + 1) + 0)
    this.client.user?.setActivity(showList.shows[randomIndex], { type: ActivityType.Watching })
  }

  public getClient = (): Client<boolean> => this.client
  public getSettings = (): Settings | undefined => this.settings.fetch()
  public getSettingsManager = (): SettingsManager => this.settings
}

// make an instance of the application class
(() => new App())()
