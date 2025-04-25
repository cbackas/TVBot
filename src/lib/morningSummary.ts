import { type APIEmbed, type Client } from "npm:discord.js"
import { type SettingsType } from "lib/settingsManager.ts"
import { getUpcomingEpisodesEmbed } from "lib/upcoming.ts"
import client from "lib/prisma.ts"
import { type Show } from "prisma-client/client.ts"
import { isTextChannel } from "lib/episodeNotifier.ts"

export async function sendMorningSummary(
  settings: SettingsType,
  c: Client,
): Promise<void> {
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

  for (const dest of settings.morningSummaryDestinations) {
    const channel = await c.channels.fetch(dest.channelId)
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
