import { Destination, Show } from "@prisma/client"
import { AnyThreadChannel, Channel, ChannelType, Client, Collection, TextChannel } from "discord.js"
import moment from "moment-timezone"
import schedule from 'node-schedule'
import { App } from "../app"
import { markMessageSent } from "./database/shows"
import client from "./prisma"
import { SettingsManager } from "./settingsManager"
import { addLeadingZeros, toRanges } from "./util"

const isTextChannel = (channel: Channel): channel is AnyThreadChannel<boolean> | TextChannel => {
  return channel.isTextBased() && !channel.isDMBased() && ![ChannelType.GuildAnnouncement, ChannelType.GuildVoice].includes(channel.type)
}

export type NotificationPayload = {
  key: string
  airDate: Date
  imdbId: string
  showName: string
  season: number
  episodeNumbers: number[]
}

/**
 * look through episodes in the db and schedule notifications to the defined destinations
 * @param app instance of the main app
 */
export const scheduleAiringMessages = async (app: App): Promise<void> => {
  const showsWithEpisodes = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false
        }
      }
    },
  })

  // convert the shows into a collection of notification payloads
  const payloadCollection = showsWithEpisodes.reduce(reduceEpisodes, new Collection<string, NotificationPayload>())

  // grab the discord client and global destinations for the schedule jobs to use
  const discord = app.getClient()
  const settingsManager = app.getSettingsManager()
  // const globalDestinations = app.getSettings()?.allEpisodes ?? []

  for (const payload of payloadCollection.values()) {
    await scheduleJob(payload, discord, settingsManager)
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

/**
 * schedule an episode notification to go out on discord at the given time
 * @param payload all the info needed to schedule a notification job
 * @param discord client needed to send the messages
 * @param globalDestinations additional destinations to send the message to
 */
const scheduleJob = async (payload: NotificationPayload, discord: Client, settingsManager: SettingsManager) => {
  const { key, airDate, imdbId: showId, showName, season, episodeNumbers } = payload

  // handle timezones
  const airDateUTC = moment.utc(airDate)
  const airDateLocal = airDateUTC.tz(process.env.TZ ?? 'America/Chicago')

  const existingJob = schedule.scheduledJobs[key]
  if (existingJob) {
    const nextInvocation: Date = (existingJob.nextInvocation() as any).toDate()

    // if the next invocation is different than the air date, reschedule the job
    if (nextInvocation.toISOString() != airDateLocal.toDate().toISOString()) {
      const job = schedule.rescheduleJob(key, airDateLocal.toDate())
      console.info(`Rescheduled Job: ${key} at ${job.nextInvocation()}`)
    }

    // if the job already existed then dont do anything else
    return
  }

  const message = getEpisodeMessage(showName, season, episodeNumbers)

  // create a scheduled event to send a message
  const newJob = schedule.scheduleJob(key, airDateLocal.toDate(), async () => {
    // grab the show destinations from the db
    const show = await client.show.findUnique({
      where: {
        imdbId: showId
      },
      select: {
        destinations: true
      }
    })

    const showDestinations = show?.destinations ?? []

    // send the message to all the channels subscribed to the show
    for (const destination of showDestinations) {
      const channel = await discord.channels.fetch(destination.channelId)
      if (!channel) throw new Error('Channel not found')

      // send message to discord
      await sendMessage(channel, message)
    }

    // build the message that's sent to the global destinations
    const channelsString = showDestinations.map(d => `<#${d.channelId}>`).join(' ')
    const globalMessage = message + ` Check out the discussions here: ${channelsString}`

    // send messages to all the global destinations
    for (const destination of settingsManager.fetch()?.allEpisodes ?? []) {
      const channel = await discord.channels.fetch(destination.channelId)
      if (!channel) throw new Error('Channel not found')

      // send message to discord
      await sendMessage(channel, globalMessage)
    }

    // mark message as sent in  the db
    await markMessageSent(showId, season, episodeNumbers)
  })

  console.info(`Scheduled Job: ${key}(${showName}) at ${newJob.nextInvocation()}} `)
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

/**
 * builds the message thats sent to discord for an airing episode
 * @param showName name of the show for the message
 * @param season name of the season for the message
 * @param episodeNumbers episodes being announced in the message
 * @returns message to send to discord
 */
const getEpisodeMessage = (showName: string, season: number, episodeNumbers: number[]) => {
  if (episodeNumbers.length <= 0) {
    throw new Error('No episodes to schedule')
  }

  if (episodeNumbers.length === 1) {
    return `**${showName} S${addLeadingZeros(season, 2)}E${addLeadingZeros(episodeNumbers[0], 2)}** is airing now!`
  }

  return `**${showName} S${addLeadingZeros(season, 2)}E${toRanges(episodeNumbers)}** is streaming somewhere now!`
}
