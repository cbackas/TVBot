import { type APIEmbed, type Client } from 'discord.js'
import { type Settings } from './settingsManager'
import { getUpcomingEpisodesEmbed } from './upcoming'
import client from './prisma'
import { type Show } from '@prisma/client'
import { isTextChannel } from './episodeNotifier'

export async function sendMorningSummary (settings: Settings, c: Client): Promise<void> {
  const shows: Show[] = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false
        }
      }
    }
  })

  const embed: APIEmbed = await getUpcomingEpisodesEmbed(shows, 1)

  for (const dest of settings.morningSummaryDestinations) {
    const channel = await c.channels.fetch(dest.channelId)
    if (channel == null || !isTextChannel(channel)) continue

    await channel.send({
      content: '',
      embeds: [embed]
    })
  }
}
