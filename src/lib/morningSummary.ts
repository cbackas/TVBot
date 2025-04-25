import { type APIEmbed } from "npm:discord.js"
import { getUpcomingEpisodesEmbed } from "lib/upcoming.ts"
import client from "lib/prisma.ts"
import { type Show } from "prisma-client/client.ts"
import { isTextChannel } from "lib/episodeNotifier.ts"
import { getClient } from "app.ts"
import { getSetting } from "database/settings.ts"

export async function sendMorningSummary(): Promise<void> {
  const destinations = await getSetting("morningSumarryDestinations")
  if (destinations.length === 0) {
    console.info("Morning summary destinations not set, skipping")
    return
  }

  const shows: Show[] = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false,
        },
      },
    },
  })

  const embed: APIEmbed = getUpcomingEpisodesEmbed(shows, 1)

  const discordClient = getClient()
  for (const dest of destinations) {
    const channel = await discordClient.channels.fetch(dest.channelId)
    if (channel == null || !isTextChannel(channel) || !channel.isSendable()) {
      console.warn(
        `Found channel ${dest.channelId} in the morning summary destinations but it is not a text channel or is not sendable`,
      )
      continue
    }

    await channel.send({
      content: "",
      embeds: [embed],
    })
  }
}
