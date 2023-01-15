import { Destination, Prisma } from "@prisma/client"
import { TextBasedChannel } from "discord.js"
import moment from "moment-timezone"
import { Episode } from "../interfaces/tvdb"
import { isThreadChannel } from "../interfaces/discord"
import client from "./prisma"
import { getTimezone } from "./timezones"
import { getSeries } from "./tvdb"

/**
 * Get a moment airdate with the specified date and time
 * @param dateStr date as string to use for the air date
 * @param timeStr time as a string to use for the airdate
 * @param timezone timezone to use for the airdate
 * @returns moment object representing the airdate
 */
const getAirDate = (dateStr: string, timeStr: string | null, timezone: string) => {
  try {
    return moment.tz(`${dateStr} ${timeStr !== null ? timeStr : '00:00'}`, timezone)
  } catch (error) {
    throw new Error(`Could not parse air date`)
  }
}

/**
 * Fetch new episodes for a show and save them in the DB
 * @param imdbId imdbid of the show to update
 * @param tvdbId tvdbId of the show to update
 */
export async function updateEpisodes(imdbId: string, tvdbId: number): Promise<void> {
  const series = await getSeries(tvdbId)
  const timezone = getTimezone(series.latestNetwork.country)

  // filter out episodes that have already aired
  // map to the episode list of objects we want to store
  const upcomingEpisodes = series.episodes
    .filter((e: Episode) => {
      if (e.aired === null) return false
      return getAirDate(e.aired, series.airsTime, timezone).toDate() > new Date()
    })
    .map((e) => {
      const airDate = getAirDate(e.aired, series.airsTime, timezone)
      const airDateUTC = airDate.utc().toDate()
      return Prisma.validator<Prisma.ShowCreateInput['episodes']>()({
        season: e.seasonNumber,
        number: e.number,
        title: e.name ?? '',
        airDate: airDateUTC
      })
    })

  // update the show with the new episodes (we can just replace all of them)
  await client.show.update({
    where: {
      imdbId: imdbId
    },
    data: {
      name: series.name,

      episodes: {
        set: upcomingEpisodes
      }
    }
  })

  console.info(`[SYNC] ${series.name} / Upcoming episodes: ${upcomingEpisodes.length}`)
}

/**
 * Updates all shows in the DB with new episodes
 */
export const checkForAiringEpisodes = async (): Promise<void> => {
  console.info('== Checking all shows for airing episodes ==')
  const shows = await client.show.findMany({
    select: {
      id: true,
      name: true,
      imdbId: true,
      tvdbId: true,
      episodes: true,
      destinations: true
    }
  })

  for (const show of shows) {
    await updateEpisodes(show.imdbId, show.tvdbId)
  }

  console.info('== Finished checking all shows for airing episodes ==')
}

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param showId id of the show to mark episodes sent in
 * @param seasonNumber season to mark episodes sent in
 * @param episodeNumbers array of episode numbers to mark as sent
 */
export async function markMessageSent(imdbId: string, seasonNumber: number, episodeNumbers: number[]): Promise<void>

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param showId id of the show to mark episodes sent in
 * @param seasonNumber season to mark episodes sent in
 * @param episodeNumber episode number(s) to mark as sent
 */
export async function markMessageSent(imdbId: string, seasonNumber: number, episodeNumber: number): Promise<void>

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param imdbId show to mark episodes as sent for
 * @param seasonNumber season to mark episodes as sent for
 * @param episodeNumber episode number(s) to mark as sent
 */
export async function markMessageSent(imdbId: string, seasonNumber: number, episodeNumber: number | number[]): Promise<void> {
  // handle overloaded function to turn params into an array
  let episodeNumbers: number[] = (Array.isArray(episodeNumber) ? episodeNumber : [episodeNumber])

  await client.show.update({
    where: {
      imdbId
    },
    data: {
      episodes: {
        updateMany: {
          where: {
            season: seasonNumber,
            number: {
              in: episodeNumbers
            }
          },
          data: {
            messageSent: true
          }
        }
      }
    }
  })
}

/**
 * Creates a new episode notification subscription for a show
 * @param imdbId imdbID for the show to subscribe to
 * @param tvdbSeriesId tvdb id for the show
 * @param seriesName name of the tv show
 * @param channel discord channel to send notifications to
 * @returns 
 */
export const createNewSubscription = async (imdbId: string, tvdbSeriesId: number, seriesName: string, channel: TextBasedChannel) => {
  return await client.show.upsert({
    where: {
      imdbId
    },
    update: {
      tvdbId: tvdbSeriesId,
      name: seriesName,
      destinations: {
        push: {
          channelId: channel.id,
          forumId: isThreadChannel(channel) ? channel.parentId : null
        }
      }
    },
    create: {
      imdbId: imdbId,
      tvdbId: tvdbSeriesId,
      name: seriesName,
      destinations: {
        set: [{
          channelId: channel.id,
          forumId: isThreadChannel(channel) ? channel.parentId : null
        }]
      }
    }
  })
}

/**
 * unsubscribe a channel from notifications for a show
 * @param imdbId imdbID for the show to remove the subscription from
 * @param channelId channel to unsubscribe the show from
 * @returns the show that was unsubscribed from
 */
export const removeSubscription = async (imdbId: string, channelId: string) => {
  return await client.show.update({
    where: {
      imdbId
    },
    data: {
      destinations: {
        deleteMany: {
          where: {
            channelId
          }
        }
      }
    }
  })
}

/**
 * unsubscribes a channel from all notifications
 * @param id id to use in the where clause
 * @param idType whether to use the channel id or forum id in the where clause, defaults to channel id
 */
export async function removeAllSubscriptions(id: string, idType: keyof Destination = 'channelId'): Promise<void> {

  await client.show.updateMany({
    data: {
      destinations: {
        deleteMany: {
          where: {
            [idType]: id
          }
        }
      }
    }
  })

  console.log('Deleted all show destinations for channel ' + id)
}

/**
 * removes all shows that have no destinations
 */
export const pruneUnsubscribedShows = async () => {
  await client.show.deleteMany({
    where: {
      destinations: {
        isEmpty: true
      }
    }
  })

  console.info('Pruned shows with no destinations')
}
