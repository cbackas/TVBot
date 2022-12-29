import { Destination } from "@prisma/client"
import { AnyThreadChannel, Channel, ChannelType, Collection, TextChannel } from "discord.js"
import moment from "moment-timezone"
import schedule from 'node-schedule'
import { App } from "../app"
import { markMessageSent } from "./database/shows"
import client from "./prisma"
import { addLeadingZeros, toRanges } from "./util"

const isTextChannel = (channel: Channel): channel is AnyThreadChannel<boolean> | TextChannel => {
  return channel.isTextBased() && !channel.isDMBased() && ![ChannelType.GuildAnnouncement, ChannelType.GuildVoice].includes(channel.type)
}

type NotificationPayload = {
  key: string
  airDate: Date
  showId: string
  showName: string
  season: number
  episodeNumbers: number[]
  destinations: Destination[]
}

export const scheduleAiringMessages = async (app: App): Promise<void> => {
  const momentUTC = moment.utc(new Date())

  const showsWithEpisodes = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false
        }
      }
    },
  })

  const payloadCollection = showsWithEpisodes.reduce((acc, show) => {
    const episodes = show.episodes.filter(e => {
      const airDate = moment.utc(e.airDate)
      return airDate.isSameOrAfter(momentUTC) && airDate.isSameOrBefore(momentUTC.clone().add('1', 'day'))
    })

    episodes.forEach(e => {
      const key = `announceEpisodes:${e.airDate.toISOString()}:${show.imdbId}:S${addLeadingZeros(e.season, 2)}`

      // grab the payload from the collection or create a new one
      const payload = acc.ensure(key, () => ({
        key,
        airDate: e.airDate,
        showId: show.imdbId,
        showName: show.name,
        season: e.season,
        episodeNumbers: [],
        destinations: show.destinations
      }))

      // add the episode number to the payload
      payload.episodeNumbers.push(e.number)
    })

    return acc
  }, new Collection<string, NotificationPayload>())

  for (const payload of payloadCollection.values()) {
    await scheduleJob(payload, app)
  }
}

// schedule a new job for the episode
const scheduleJob = async (payload: NotificationPayload, app: App) => {
  const { key, airDate, showId, showName, season, episodeNumbers, destinations } = payload

  const airDateUTC = moment.utc(airDate)
  const airDateLocal = airDateUTC.tz(process.env.TZ ?? 'America/Chicago')

  const existingJob = schedule.scheduledJobs[key]
  if (existingJob) {
    const nextInvocation: Date = (existingJob.nextInvocation() as any).toDate()
    if (nextInvocation.toISOString() != airDateLocal.toDate().toISOString()) {
      const job = schedule.rescheduleJob(key, airDateLocal.toDate())
      console.info(`Rescheduled Job: ${key} at ${job.nextInvocation()}`)
    }
    return
  }

  const message = getEpisodeMessage(showName, season, episodeNumbers)

  // create a scheduled event to send a message
  const newJob = schedule.scheduleJob(key, airDateLocal.toDate(), async () => {
    const discord = app.getClient()

    // add the default destination to the list of destinations
    destinations.concat(app.getSettings()?.allEpisodes ?? [])

    for (const destination of destinations) {
      const channel = await discord.channels.fetch(destination.channelId)
      if (!channel) throw new Error('Channel not found')

      // send message to discord
      await sendMessage(channel, message)
      // mark message as sent in  the db
      await markMessageSent(showId, season, episodeNumbers)
    }
  })

  console.info(`Scheduled Job: ${key} (${showName}) at ${newJob.nextInvocation()}}`)
}

/**
 * Send a message to a discord channel
 * @param channel where to send the message
 * @param message what to send
 */
const sendMessage = async (channel: Channel, message: string) => {
  if (!isTextChannel(channel)) throw new Error('Channel is not a text channel')

  // send discord message
  await channel.send(message)

  console.info(`Message Sent: ${message} `)
}

const getEpisodeMessage = (showName: string, season: number, episodeNumbers: number[]) => {
  if (episodeNumbers.length <= 0) {
    throw new Error('No episodes to schedule')
  }

  if (episodeNumbers.length === 1) {
    return `${showName} S${addLeadingZeros(season, 2)}E${addLeadingZeros(episodeNumbers[0], 2)} is airing now!`
  }

  const episodeRange = toRanges(episodeNumbers)

  return `${showName} S${addLeadingZeros(season, 2)}E${episodeRange} is streaming somewhere now!`
}
