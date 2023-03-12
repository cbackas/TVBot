import { type Show } from '@prisma/client'
import { type APIEmbed, Collection, type APIEmbedField } from 'discord.js'
import moment from 'moment'
import { type NotificationPayload } from './episodeNotifier'
import { addLeadingZeros, toRanges } from './util'

interface UpcomingEpisodeMessages {
  prefix: string
  empty: string
  messages: string[]
  embedFields: APIEmbedField[]
}

async function getShowMessages (shows: Show[], days: number = 1): Promise<UpcomingEpisodeMessages> {
  if (days <= 0) throw new Error('days must be greater than 0')

  const payloadCollection = reduceEpisodes(shows, days)

  const sortedPayloads = payloadCollection
    .sort((p1, p2) => {
      return moment.utc(p1.airDate).unix() - moment.utc(p2.airDate).unix()
    })

  const messages = sortedPayloads
    .map(payload => {
      const seasonNumber = addLeadingZeros(payload.season, 2)
      const episodeNumbers = toRanges(payload.episodeNumbers)
      const message = `**${payload.showName}** S${seasonNumber}E${episodeNumbers.join(',')} - <t:${moment.utc(payload.airDate).unix()}:R>`
      return message
    })

  const embedFields = sortedPayloads
    .reduce((acc, payload) => {
      const seasonNumber = addLeadingZeros(payload.season, 2)
      const episodeNumbers = toRanges(payload.episodeNumbers)
      const message = `**${payload.showName}** S${seasonNumber}E${episodeNumbers.join(',')} - <t:${moment.utc(payload.airDate).unix()}:R>`
      const airDate = moment.utc(payload.airDate).tz(process.env.TZ ?? 'America/Chicago')
      acc.ensure(airDate.format('dddd - Do of MMMM'), () => []).push(message)
      return acc
    }, new Collection<string, string[]>())
    .map((messages, airDate) => {
      return {
        name: airDate,
        value: messages.join('\n')
      }
    })

  let prefixString: string = 'Shows airing '
  let emptyString: string = 'No shows airing '

  if (days === 1) {
    prefixString += 'in the next day'
    emptyString += 'today'
  } else if (days === 7) {
    prefixString += 'this week'
    emptyString += 'this week'
  } else {
    prefixString += `in the next ${days} days`
    emptyString += `in the next ${days} days`
  }

  return {
    prefix: prefixString,
    empty: emptyString,
    embedFields,
    messages
  }
}

/**
 * Gets upcoming episodes for the next X days and returns a string message to send to users
 * @param days number of days to look ahead
 * @returns a string message to send to Discord
 */
export async function getUpcomingEpisodesMessage (shows: Show[], days: number = 1): Promise<string> {
  const messages = await getShowMessages(shows, days)

  return messages.messages.length >= 1 ? `${messages.prefix}:\n\n${messages.messages.join('\n')}` : messages.empty
}

export async function getUpcomingEpisodesEmbed (shows: Show[], days: number = 1): Promise<APIEmbed> {
  const messages = await getShowMessages(shows, days)

  return {
    title: messages.prefix,
    fields: messages.embedFields
  }
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
