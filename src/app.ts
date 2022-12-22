import * as dotenv from 'dotenv'
import schedule from 'node-schedule'
import { ActivityType, Client, Collection, Events, GatewayIntentBits, REST, RESTPostAPIChatInputApplicationCommandsJSONBody, RESTPostAPIContextMenuApplicationCommandsJSONBody, Routes } from 'discord.js'
import { Command } from './interfaces/command'
import client from './lib/prisma'
import { Settings } from '@prisma/client'
import { checkForAiringEpisodes, scheduleAiringMessages } from './lib/episodeNotifier'

dotenv.config()

type Getter<TInput> = { command: TInput }

/**
 * all commands required here will be registered on app startup
 */
const commandModules: Getter<Command>[] = [
  require('./commands/post'),
  require('./commands/link'),
  require('./commands/settings')
]

/**
 * The main bot application
 */
export class App {
  private client: Client
  private commands = new Collection<string, Command>()
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

    this.init()
  }

  /**
   * Async init function for app
   */
  private init = async (): Promise<void> => {
    await this.loadSettings()
    await this.registerCommands()
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
    this.client.on(Events.InteractionCreate, async (interaction) => {
      if (!interaction.isChatInputCommand()) return

      const { commandName } = interaction

      const command = this.commands.get(commandName)
      if (command === undefined) return

      console.log(`Recieved Command: ${command.data.name}`)

      await interaction.deferReply({ ephemeral: true })

      try {
        await command.execute(this, interaction)
      } catch (error) {
        console.error(error)
        await interaction.editReply('There was an error while executing this command!')
      }
    })

    this.client.on(Events.InteractionCreate, interaction => {
      if (!interaction.isUserContextMenuCommand()) return;
      console.log(interaction);
    })
  }

  /**
   * Register all commands with Discord
   */
  private registerCommands = async (): Promise<void> => {
    type SlashCommandData = RESTPostAPIChatInputApplicationCommandsJSONBody | RESTPostAPIContextMenuApplicationCommandsJSONBody
    const slashCommandData: SlashCommandData[] = []

    // loop through command modules
    // build command collection and slash command object used for discord command registration
    for (const module of commandModules) {
      const command = module.command
      this.commands.set(command.data.name, command)
      slashCommandData.push(command.data.toJSON())
    }

    // when testing locally you dont always need to register commands
    if (process.env.REGISTER_COMMANDS === 'false') return

    try {
      console.log('Starting to register slash commands')
      const rest = new REST({ version: '10' }).setToken(this.token)
      await rest.put(Routes.applicationGuildCommands(this.clientId, this.guildId), { body: slashCommandData })
      console.log('Slash commands registered: ' + this.commands.map((cmd) => cmd.data.name))
    } catch (error) {
      console.error(error)
    }
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
