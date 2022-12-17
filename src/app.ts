import { Client, GatewayIntentBits } from 'discord.js'
import { registerCommands } from './command'

import * as dotenv from 'dotenv'
dotenv.config()

const start = (): void => {
  const client = new Client({ intents: [GatewayIntentBits.Guilds] })

  client.on('ready', () => {
    if (client.user !== null) console.log(`Logged in as ${client.user.tag}!`)
  })

  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return

    const { commandName } = interaction

    console.log(`Command: ${commandName}`)

    if (commandName === 'ping') {
      await interaction.reply('Pong!')
    }
  })

  const token = process.env.DISCORD_TOKEN

  if (token === undefined) throw new Error('DISCORD_TOKEN is not defined')

  void client.login(token)
}

await registerCommands() // sends all the slash commands over to Discord so users can see them
start() // start the bot
