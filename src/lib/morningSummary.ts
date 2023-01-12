import { Show } from "@prisma/client";
import { Client, Collection } from "discord.js";
import moment from "moment";
import { NotificationPayload } from "./episodeNotifier";
import client from "./prisma";
import { Settings } from "./settingsManager";
import { addLeadingZeros, toRanges } from "./util";

export async function sendMorningSummary(settings: Settings, c: Client) {
  const showsWithEpisodes = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false
        }
      }
    },
  })

  const payloadCollection = showsWithEpisodes.reduce(reduceEpisodes, new Collection<string, NotificationPayload>())

  const messages = payloadCollection.map((payload) => {
    const seasonNumber = addLeadingZeros(payload.season, 2)
    const episodeNumbers = toRanges(payload.episodeNumbers)
    return `**${payload.showName}** S${seasonNumber}E${episodeNumbers} - <t:${moment.utc(payload.airDate).unix()}:R>`
  })

  for (const dest of settings.morningSummaryDestinations) {
    const channel = await c.channels.fetch(dest.channelId)
    if (!channel || !channel.isTextBased()) continue

    const message = messages.length >= 1 ? `Shows airing today:\n\n${messages.join('\n')}` : 'No shows airing today.'

    await channel.send(message)
  }
}

/**
 * reduce function that converts a list of shows into a collection of notification payloads
 * @param acc accumulator, a collection of notification payloads
 * @param show current show to process
 * @returns collection of notification payloads
 */
export const reduceEpisodes = (acc: Collection<string, NotificationPayload>, show: Show) => {
  const momentUTC = moment.utc(new Date())

  for (const e of show.episodes) {
    const airDate = moment.utc(e.airDate)
    const inTimeWindow = airDate.isSameOrAfter(momentUTC) && airDate.isSameOrBefore(momentUTC.clone().add('1', 'day'))

    if (!inTimeWindow) continue

    const airDateString = moment.utc(e.airDate)
      .tz(process.env.TZ ?? 'America/Chicago')
      .format('YYYY-MM-DD@HH:mm')

    const key = `announceEpisodes:${airDateString}:${show.imdbId}:S${addLeadingZeros(e.season, 2)}`

    // define the default payload to use if one doesn't exist in the collection
    const defaultPayload: NotificationPayload = {
      key,
      airDate: e.airDate,
      imdbId: show.imdbId,
      showName: show.name,
      season: e.season,
      episodeNumbers: [] // it has an emtpy array of episode numbers because it will be filled in later
    }

    // grab the payload from the collection or create a new one
    const payload = acc.ensure(key, () => (defaultPayload))

    // add the episode number to the payload
    payload.episodeNumbers.push(e.number)
  }

  return acc
}
