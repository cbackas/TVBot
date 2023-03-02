import { ChannelType, type Client } from 'discord.js'
import { type Settings } from './settingsManager'
import { getUpcomingEpisodes } from './upcoming'

export async function sendMorningSummary (settings: Settings, c: Client): Promise<void> {
  const message = await getUpcomingEpisodes(1)

  for (const dest of settings.morningSummaryDestinations) {
    const channel = await c.channels.fetch(dest.channelId)
    if ((channel == null) || !channel.isTextBased() || channel.type === ChannelType.GuildStageVoice) continue

    await channel.send(message)
  }
}
