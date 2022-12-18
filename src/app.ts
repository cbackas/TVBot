import * as dotenv from 'dotenv'
import { Client, Collection, Events, GatewayIntentBits, REST, RESTPostAPIChatInputApplicationCommandsJSONBody, Routes } from 'discord.js'
import { Command } from './interfaces/command'

dotenv.config()

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

  private init = async (): Promise<void> => {
    if (process.env.REGISTER_COMMANDS !== 'false') {
      await this.registerCommands()
    }

    this.start()
  }

  private start = (): void => {
    this.client = new Client({ intents: [GatewayIntentBits.Guilds] })

    this.client.on('ready', () => {
      const { user } = this.client
      if (user !== null) console.log(`Logged in as ${user.tag}!`)
    })

    this.client.on(Events.InteractionCreate, async (interaction) => {
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

    void this.client.login(this.token)
  }

  private registerCommands = async (): Promise<void> => {
    type Getter<TInput> = { default: TInput }

    const commandModules: Getter<Command>[] = [
      require('./commands/ping')
    ]

    const slashCommandData: RESTPostAPIChatInputApplicationCommandsJSONBody[] = []

    for (const module of commandModules) {
      const command = module.default
      this.commands.set(command.data.name, command)
      slashCommandData.push(command.data.toJSON())
    }

    const rest = new REST({ version: '10' }).setToken(this.token)

    try {
      console.log('Starting to register slash commands')
      await rest.put(Routes.applicationCommands(this.clientId), { body: slashCommandData })
      console.log('Slash commands registered: ' + this.commands.map((cmd) => cmd.data.name))
    } catch (error) {
      console.error(error)
    }
  }

  public getClient = (): Client => {
    return this.client
  }
}

// make an instance of the application class
new App()
