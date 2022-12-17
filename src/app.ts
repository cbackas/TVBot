import { Client, GatewayIntentBits } from 'discord.js'

const client = new Client({ intents: [GatewayIntentBits.Guilds] })

client.on('ready', () => {
  if (client.user !== null) console.log(`Logged in as ${client.user.tag}!`)
})

client.on('interactionCreate', async (interaction) => {
  if (!interaction.isChatInputCommand()) return

  if (interaction.commandName === 'ping') {
    await interaction.reply('Pong!')
  }
})

const token = process.env.DISCORD_TOKEN

if (token === undefined) throw new Error('DISCORD_TOKEN is not defined')

void client.login(token)
