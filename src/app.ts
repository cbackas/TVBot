import * as dotenv from 'dotenv'
import schedule from 'node-schedule'
import { ActivityType, ApplicationCommandType, Client, Collection, ContextMenuCommandBuilder, Events, GatewayIntentBits, REST, RESTPostAPIChatInputApplicationCommandsJSONBody, RESTPostAPIContextMenuApplicationCommandsJSONBody, Routes } from 'discord.js'
import { Command } from './interfaces/command'

dotenv.config()

type Getter<TInput> = { default: TInput }

/**
 * all commands required here will be registered on app startup
 */
const commandModules: Getter<Command>[] = [
  require('./commands/ping')
]

/**
 * The main bot application
 */
class App {
  private client: Client
  private commands = new Collection<string, Command>()

  private token
  private clientId

  constructor() {
    if (process.env.DISCORD_TOKEN === undefined) throw new Error('DISCORD_TOKEN is not defined')
    if (process.env.DISCORD_CLIENT_ID === undefined) throw new Error('DISCORD_CLIENT_ID is not defined')

    this.token = process.env.DISCORD_TOKEN
    this.clientId = process.env.DISCORD_CLIENT_ID

    this.init()
  }

  /**
   * Async init function for app
   */
  private init = async (): Promise<void> => {
      await this.registerCommands()
    this.startBot()
  }

  /**
   * Start the bot and register listeners
   */
  private startBot = (): void => {
    this.client = new Client({ intents: [GatewayIntentBits.Guilds] })

    this.client.on(Events.ClientReady, () => {
      const { user } = this.client
      if (user !== null) console.log(`Logged in as ${user.tag}!`)

      this.randomWatchingActivity()
    })

    this.startEventListeners()

    void this.client.login(this.token)
  }

  private startEventListeners = (): void => {
    this.client.on(Events.InteractionCreate, async (interaction) => {
      // if (interaction.isCommand()) console.log('cmd')
      if (!interaction.isChatInputCommand()) return

      const { commandName } = interaction

      const command = this.commands.get(commandName)
      if (command === undefined) return

      console.log(`Recieved Command: ${command.data.name}`)
      
      try {
        await command.execute(interaction)
      } catch (error) {
        console.error(error)
        await interaction.reply({ content: 'There was an error while executing this command!', ephemeral: true })
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
    // const slashCommandData = []

    // loop through command modules
    // build command collection and slash command object used for discord command registration
    for (const module of commandModules) {
      const command = module.default
      this.commands.set(command.data.name, command)
      slashCommandData.push(command.data.toJSON())
    }

    slashCommandData.push(new ContextMenuCommandBuilder()
      .setName('Test Command')
      .setType(ApplicationCommandType.User)
    )

    // when testing locally you dont always need to register commands
    if (process.env.REGISTER_COMMANDS === 'false') return
    
    try {
      console.log('Starting to register slash commands')
      const rest = new REST({ version: '10' }).setToken(this.token)
      await rest.put(Routes.applicationCommands(this.clientId), { body: slashCommandData })
      console.log('Slash commands registered: ' + this.commands.map((cmd) => cmd.data.name))
    } catch (error) {
      console.error(error)
    }
  }

  private startScheduler = (): void => {
    schedule.scheduleJob('* 15 * * *', () => {
      this.randomWatchingActivity()
    })
  }

  private randomWatchingActivity = (): void => {
    const showList: { shows: string[] } = require('../assets/shows.json')
    const randomIndex = Math.floor(Math.random() * (showList.shows.length - 0 + 1) + 0)
    this.client.user?.setActivity(showList.shows[randomIndex], { type: ActivityType.Watching })
  }

  public getClient = (): Client => {
    return this.client
  }
}

// make an instance of the application class
new App()
