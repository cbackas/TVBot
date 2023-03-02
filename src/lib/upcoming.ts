import { type Show } from '@prisma/client'
import { Collection } from 'discord.js'
import moment from 'moment'
import { type NotificationPayload } from './episodeNotifier'
import { addLeadingZeros, toRanges } from './util'
import client from './prisma'

/**
 * Gets upcoming episodes for the next X days and returns a string message to send to users
 * @param days number of days to look ahead
 * @returns a string message to send to Discord
 */
export async function getUpcomingEpisodes (days: number = 1): Promise<string> {
  const shows = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false
        }
      }
    }
  })

  const payloadCollection = reduceEpisodes(shows, days)

  const messages = payloadCollection
    .sort((p1, p2) => {
      return moment.utc(p1.airDate).unix() - moment.utc(p2.airDate).unix()
    })
    .map((payload) => {
      const seasonNumber = addLeadingZeros(payload.season, 2)
      const episodeNumbers = toRanges(payload.episodeNumbers)
      return `**${payload.showName}** S${seasonNumber}E${episodeNumbers.join(',')} - <t:${moment.utc(payload.airDate).unix()}:R>`
    })

  return messages.length >= 1 ? `Shows airing today:\n\n${messages.join('\n')}` : 'No shows airing today.'
}

/**
 * reduce function that converts a list of shows into a collection of notification payloads
 * @param shows list of shows to process
 * @param days number of days to look ahead
 * @returns collection of notification payloads
 */
function reduceEpisodes (shows: Show[], days: number = 1): Collection<string, NotificationPayload> {
  return shows.reduce((acc: Collection<string, NotificationPayload>, show: Show) => {
    const momentUTC = moment.utc(new Date())

    for (const e of show.episodes) {
      const airDate = moment.utc(e.airDate)
      const inTimeWindow = airDate.isSameOrAfter(momentUTC) && airDate.isSameOrBefore(momentUTC.clone().add(days, 'day'))

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
  }, new Collection<string, NotificationPayload>())
}
