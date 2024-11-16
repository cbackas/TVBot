import { type Show } from "npm:@prisma/client"
import {
  type AnyThreadChannel,
  type Channel,
  ChannelType,
  type Client,
  Collection,
  type TextChannel,
} from "npm:discord.js"
import moment from "npm:moment-timezone"
import { type App } from "app.ts"
import { markMessageSent } from "lib/shows.ts"
import client from "lib/prisma.ts"
import { type Settings } from "lib/settingsManager.ts"
import { addLeadingZeros, toRanges } from "lib/util.ts"

export function isTextChannel(
  channel: Channel,
): channel is AnyThreadChannel<boolean> | TextChannel {
  return channel.isTextBased() && !channel.isDMBased() &&
    ![ChannelType.GuildVoice].includes(channel.type)
}

export interface NotificationPayload {
  key: string
  timestamp: number
  imdbId: string
  showName: string
  season: number
  episodeNumbers: number[]
  destinations: Show["destinations"]
}

type PayloadCollection = Collection<string, NotificationPayload>

/**
 * Send messages for all the shows that have episodes airing in the next few minutes
 * @param app the app instance
 * @returns a promise that resolves when all the messages have been sent
 */
export async function sendAiringMessages(app: App): Promise<void> {
  const discord = app.getClient()
  const globalDestinations = app.getSettingsManager().fetch()?.allEpisodes ?? []

  const payloadCollection = await getShowPayloads()
  for (const payload of payloadCollection.values()) {
    await sendNotificationPayload(payload, discord, globalDestinations)
  }
}

/**
 * Get all the shows that have episodes airing in the next x minutes
 * @param minutes how many minutes in the future to look for shows
 * @returns a collection of payloads for each show that has an episode airing in the next x minutes
 */
async function getShowPayloads(
  minutes: number = 5,
): Promise<PayloadCollection> {
  const nowUtc = moment.utc()
  const minutesFromNow = nowUtc.add(minutes, "minutes")

  const showsWithEpisodes: Show[] = await client.show.findMany({
    where: {
      episodes: {
        some: {
          messageSent: false,
          airDate: {
            lte: minutesFromNow.toDate(),
          },
        },
      },
    },
  })

  // convert the shows into a collection of notification payloads
  const payloadCollection: PayloadCollection = showsWithEpisodes.reduce(
    (
      acc: Collection<string, NotificationPayload>,
      show: Show,
    ): Collection<string, NotificationPayload> => {
      const momentUTC = moment.utc(new Date())

      for (const e of show.episodes) {
        const airDate = moment.utc(e.airDate)
        const inTimeWindow = airDate.isSameOrAfter(momentUTC) &&
          airDate.isSameOrBefore(momentUTC.clone().add(minutes, "minutes"))

        if (!inTimeWindow) continue

        const key = `announceEpisodes:${show.imdbId}:S${
          addLeadingZeros(e.season, 2)
        }`

        // define the default payload to use if one doesn't exist in the collection
        const defaultPayload: NotificationPayload = {
          key,
          timestamp: airDate.unix(),
          imdbId: show.imdbId,
          showName: show.name,
          season: e.season,
          episodeNumbers: [], // it has an emtpy array of episode numbers because it will be filled in later
          destinations: show.destinations,
        }

        // grab the payload from the collection or create a new one
        const payload = acc.ensure(key, () => defaultPayload)

        // add the episode number to the payload
        payload.episodeNumbers.push(e.number)
      }

      return acc
    },
    new Collection<string, NotificationPayload>(),
  )

  return payloadCollection
}

/**
 * send discord messages to epissode and global destinations with info about the episode(s)
 * @param payload all the info needed to schedule a notification job
 * @param discord client needed to send the messages
 * @param globalDestinations additional destinations to send the message to
 */
async function sendNotificationPayload(
  payload: NotificationPayload,
  discord: Client,
  globalDestinations: Settings["allEpisodes"],
): Promise<void> {
  const message = getEpisodeMessage(
    payload.showName,
    payload.season,
    payload.episodeNumbers,
    payload.timestamp,
  )

  // send the message to all the channels subscribed to the show
  for (const destination of payload.destinations) {
    try {
      const channel = await discord.channels.fetch(destination.channelId)
      if (channel == null) throw new Error("Channel not found")

      // send message to discord
      await sendMessage(channel, message)
    } catch (e) {
      console.error("Error sending message to destination", e)
    }
  }

  // build the message that's sent to the global destinations
  const channelsString = payload.destinations.map((d) => `<#${d.channelId}>`)
    .join(" ")
  const globalMessage = message +
    ` Check out the discussions here: ${channelsString}`

  // send messages to all the global destinations
  for (const destination of globalDestinations) {
    try {
      const channel = await discord.channels.fetch(destination.channelId)
      if (channel == null) throw new Error("Channel not found")

      // send message to discord
      await sendMessage(channel, globalMessage)
    } catch (e) {
      console.error("Error sending message to global destination", e)
    }
  }

  // mark message as sent in  the db
  await markMessageSent(
    payload.imdbId,
    payload.season,
    payload.episodeNumbers,
  )
}

/**
 * Send a message to a discord channel
 * @param channel where to send the message
 * @param message what to send
 */
async function sendMessage(channel: Channel, message: string): Promise<void> {
  if (!isTextChannel(channel)) throw new Error("Channel is not a text channel")

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
 * @throws if there are no episodes to schedule
 */
function getEpisodeMessage(
  showName: string,
  season: number,
  episodeNumbers: number[],
  timestamp: number,
): string {
  if (episodeNumbers.length <= 0) {
    throw new Error("No episodes to schedule")
  }

  if (episodeNumbers.length === 1) {
    return `**${showName} S${addLeadingZeros(season, 2)}E${
      addLeadingZeros(episodeNumbers[0], 2)
    }** is airing <t:${timestamp}:R>`
  }

  return `**${showName} S${addLeadingZeros(season, 2)}E${
    toRanges(episodeNumbers).join(",")
  }** is streaming somewhere <t:${timestamp}:R>!`
}
