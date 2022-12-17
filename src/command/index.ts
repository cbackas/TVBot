import { REST, Routes } from 'discord.js'

const commands = [
  {
    name: 'ping',
    description: 'Replies with Pong!'
  }
]

export const registerCommands = async (): Promise<void> => {
  const token = process.env.DISCORD_TOKEN
  const clientId = process.env.DISCORD_CLIENT_ID
  if (token === undefined) throw new Error('DISCORD_TOKEN is not defined')
  if (clientId === undefined) throw new Error('DISCORD_CLIENT_ID is not defined')

  const rest = new REST({ version: '10' }).setToken(token)

  try {
    console.log('Started refreshing application (/) commands.')

    await rest.put(Routes.applicationCommands(clientId), { body: commands })

    console.log('Successfully reloaded application (/) commands.')
  } catch (error) {
    console.error(error)
  }
}
