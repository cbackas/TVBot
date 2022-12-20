import { Prisma } from "@prisma/client"
import { AnyThreadChannel, Channel, ChannelType, Collection, TextChannel } from "discord.js"
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
  // get all episodes in the DB that are airing in the next 24 hours
  const episodes = await client.episode.findMany({
    where: {
      airDate: {
        gte: new Date(),
        lte: new Date(Date.now() + 1000 * 60 * 60 * 24)
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
      for (const [season, seasonEpisodes] of airDateEpisodes) {
        // create a unique key for the job
        const key = `announceEpisodes:${airDateString}:${showId}:S${addLeadingZeros(season, 2)}`
        const airDate = seasonEpisodes[0].airDate

        const job = schedule.scheduledJobs[key]
        if (!job) {
          await scheduleJob(key, airDate, seasonEpisodes, app)
          console.info(`Scheduled Job: ${key} at ${airDateString}`)
          continue
        }

        if (job.nextInvocation() != airDate) {
          console.info(`Rescheduled Job: ${key} at ${airDateString}`)
          schedule.rescheduleJob(key, airDate)
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
  schedule.scheduleJob(key, airDate, async () => {
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

