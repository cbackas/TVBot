import * as dotenv from 'dotenv'
import schedule from 'node-schedule'
import { ActivityType, Client, Events, GatewayIntentBits } from 'discord.js'
import client from './lib/prisma'
import { Settings } from '@prisma/client'
import { checkForAiringEpisodes, scheduleAiringMessages } from './lib/episodeNotifier'
import { CommandManager } from './lib/commandManager'

dotenv.config()

/**
 * The main bot application
 */
export class App {
  private client: Client
  private commands: CommandManager
  private settings: Settings[] = []

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

    this.init()
  }

  /**
   * Async init function for app
   */
  private init = async (): Promise<void> => {
    await this.loadSettings()
    await this.commands.registerCommands()
    this.startBot()
  }

  public loadSettings = async (): Promise<void> => {
    const settings = await client.settings.findMany({
      select: {
        key: true,
        value: true
      }
    })

    this.settings = settings
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
      await checkForAiringEpisodes()
      scheduleAiringMessages(this)

      schedule.scheduleJob('lifecycle:1hour:updateBotActivity', '15 * * * *', () => {
        this.randomWatchingActivity()
      })

      schedule.scheduleJob('lifecycle:4hours:fetchEpisoded', '0 */4 * * *', async () => {
        await checkForAiringEpisodes()
        scheduleAiringMessages(this)
      })
    })

    this.startEventListeners()

    void this.client.login(this.token)
  }

  /**
   * Starts event listeners for commands and interactions
   */
  private startEventListeners = (): void => {
    this.client.on(Events.InteractionCreate, this.commands.interactionHandler)

    this.client.on(Events.InteractionCreate, interaction => {
      if (!interaction.isUserContextMenuCommand()) return;
      console.log(interaction);
    })
  }

  private randomWatchingActivity = (): void => {
    const showList: { shows: string[] } = require('../assets/shows.json')
    const randomIndex = Math.floor(Math.random() * (showList.shows.length - 0 + 1) + 0)
    this.client.user?.setActivity(showList.shows[randomIndex], { type: ActivityType.Watching })
  }

  public getClient = (): Client => this.client
  public getSettings = (): Settings[] => this.settings
}

// make an instance of the application class
new App()
