import { Prisma } from "@prisma/client"
import { AnyThreadChannel, Channel, ChannelType, Collection, TextChannel } from "discord.js"
import moment from "moment-timezone"
import schedule from 'node-schedule'
import { App } from "../app"
import { markMessageSent, updateDBEpisodes } from "./dbManager"
import client from "./prisma"
import { addLeadingZeros, toRanges } from "./util"

const EpisodeValidator = Prisma.validator<Prisma.EpisodeArgs>()({
  select: {
    showId: true,
    season: true,
    number: true,
    title: true,
    airDate: true,
    messageSent: false,
    show: {
      select: {
        id: true,
        name: true,
        ShowDestination: {
          select: {
            channelId: true,
            forumId: true,
            channelType: true
          }
        }
      }
    }
  }
})

type Episode = Prisma.EpisodeGetPayload<typeof EpisodeValidator>

const isTextChannel = (channel: Channel): channel is AnyThreadChannel<boolean> | TextChannel => {
  return channel.isTextBased() && !channel.isDMBased() && ![ChannelType.GuildAnnouncement, ChannelType.GuildVoice].includes(channel.type)
}

export const checkForAiringEpisodes = async (): Promise<void> => {
  console.info('== Starting scheduled check for airing episodes ==')
  const shows = await client.show.findMany({
    select: {
      id: true,
      name: true,
      imdbId: true,
      tvdbId: true,
      Episode: {
        select: {
          showId: true,
          season: true,
          number: true,
          title: true,
          airDate: true,
        }
      }
    }
  })

  for (const show of shows) {
    await updateDBEpisodes(show)
  }

  console.info('== Finished scheduled check for airing episodes ==')
}

export const scheduleAiringMessages = async (app: App): Promise<void> => {
  const momentUTC = moment.utc(new Date())
  // get all episodes in the DB that are airing in the next 24 hours
  const episodes = await client.episode.findMany({
    where: {
      airDate: {
        gte: momentUTC.toDate(),
        lte: momentUTC.add(1, 'day').toDate()
      },
      messageSent: false
    },
    select: EpisodeValidator.select
  })

  type ShowCollection = Collection<string, DateTimeSelection>
  type DateTimeSelection = Collection<string, SeasonCollection>
  type SeasonCollection = Collection<number, Episode[]>

  const shows: ShowCollection = new Collection()

  // group episodes by show, air date, and season
  for (const episode of episodes) {
    shows.ensure(episode.showId, () => new Collection())
      .ensure(episode.airDate.toISOString(), () => new Collection())
      .ensure(episode.season, () => [])
      .push(episode)
  }

  // schedule jobs for each episode group
  for (const [showId, showEpisodes] of shows) {
    for (const [airDateString, airDateEpisodes] of showEpisodes) {
      const airDateUTC = moment.utc(airDateString)
      const airDateLocal = airDateUTC.tz(process.env.TZ ?? 'America/Chicago')
      for (const [season, seasonEpisodes] of airDateEpisodes) {
        // create a unique key for the job
        const key = `announceEpisodes:${airDateString}:${showId}:S${addLeadingZeros(season, 2)}`

        const existingJob = schedule.scheduledJobs[key]
        if (!existingJob) {
          const newJob = await scheduleJob(key, airDateLocal.toDate(), seasonEpisodes, app)
          console.info(`Scheduled Job: ${key} (${seasonEpisodes[0].show.name}) at ${newJob.nextInvocation()}}`)
          continue
        }

        if (existingJob.nextInvocation() != airDateLocal.toDate()) {
          const job = schedule.rescheduleJob(key, airDateLocal.toDate())
          console.info(`Rescheduled Job: ${key} at ${job.nextInvocation()}`)
        }
      }
    }
  }
}

// schedule a new job for the episode
const scheduleJob = async (key: string, airDate: Date, episodes: Episode[], app: App) => {
  let message: string

  if (episodes.length <= 0) {
    throw new Error('No episodes to schedule')
  }

  if (episodes.length === 1) {
    const episode = episodes[0]
    message = `${episode.show.name} S${addLeadingZeros(episode.season, 2)}E${addLeadingZeros(episode.number, 2)} is airing now!`
  }

  const { showId, season, show } = episodes[0]
  const episodeNumbers = episodes.map(e => e.number)
  const episodeRange = toRanges(episodeNumbers)

  message = `${show.name} S${addLeadingZeros(season, 2)}E${episodeRange} is streaming somewhere now!`

  // create a scheduled event to send a message
  return schedule.scheduleJob(key, airDate, async () => {
    const discord = app.getClient()

    // send a message to each channel the show is being tracked in
    for (const destination of show.ShowDestination) {
      const channel = await discord.channels.fetch(destination.channelId)

      if (!channel) throw new Error('Channel not found')
      if (!isTextChannel(channel)) throw new Error('Channel is not a text channel')

      // send message
      await channel.send(message)

      // mark message as sent in  the db
      await markMessageSent(showId, season, episodeNumbers)

      console.info(`Message Sent: ${message} `)
    }
  })
}

