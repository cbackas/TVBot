import { type Destination, Prisma, type Show } from '@prisma/client'
import { type TextBasedChannel } from 'discord.js'
import moment, { type Moment } from 'moment-timezone'
import { isThreadChannel } from '../interfaces/discord'
import client from './prisma'
import { getTimezone } from './timezones'
import { getSeries } from './tvdb'
import { type EpisodeBaseRecord, type SeriesExtendedRecord } from '../interfaces/tvdb.generated'

/**
 * Get a moment airdate with the specified date and time
 * @param dateStr date as string to use for the air date
 * @param timeStr time as a string to use for the airdate
 * @param timezone timezone to use for the airdate
 * @returns moment object representing the airdate
 */
function getAirDate (dateStr: string, timeStr: string | null, timezone: string): Moment {
  try {
    return moment.tz(`${dateStr} ${timeStr !== null ? timeStr : '00:00'}`, timezone)
  } catch (error) {
    throw new Error('Could not parse air date')
  }
}

/**
 * Fetch new episodes for a show and save them in the DB
 * @param imdbId imdbid of the show to update
 * @param tvdbId tvdbId of the show to update
 * @param providedSeries optional series data to use instead of fetching it
 */
export async function updateEpisodes (imdbId: string, tvdbId: number, providedSeries?: SeriesExtendedRecord): Promise<void> {
  // if the caller already has series data for whatever reason and provided it, just use that
  const series: SeriesExtendedRecord | undefined = providedSeries ?? (await getSeries(tvdbId))

  // if we still dont have series data, throw an error
  if (series == null) {
    throw new Error(`Could not fetch series data for ${imdbId}`)
  }

  const timezone = getTimezone(series.latestNetwork?.country ?? 'usa')
  const airsTime = series.airsTime

  // filter out episodes that have already aired
  // map to the episode list of objects we want to store
  const upcomingEpisodes = series.episodes
    .filter((e: EpisodeBaseRecord) => {
      if (e.aired == null) return false
      return getAirDate(e.aired, airsTime, timezone).toDate() > new Date()
    })
    .map((e) => {
      if (e.aired == null) throw new Error('Episode has no air date')

      const airDate = getAirDate(e.aired, airsTime, timezone)
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
      imdbId
    },
    data: {
      name: series.name,

      episodes: {
        set: upcomingEpisodes
      }
    }
  })

  console.info(`[Get Episode Data] ${series.name} / Upcoming episodes: ${upcomingEpisodes.length}`)
}

/**
 * Updates all shows in the DB with new episodes
 */
export async function checkForAiringEpisodes (): Promise<void> {
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
    try {
      await updateEpisodes(show.imdbId, show.tvdbId)
    } catch (error) {
      console.error(`Error updating episodes for ${show.name} (${show.imdbId})`)
    }
  }

  console.info('== Finished checking all shows for airing episodes ==')
}

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param showId id of the show to mark episodes sent in
 * @param seasonNumber season to mark episodes sent in
 * @param episodeNumbers array of episode numbers to mark as sent
 */
export async function markMessageSent (imdbId: string, seasonNumber: number, episodeNumbers: number[]): Promise<void>

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param showId id of the show to mark episodes sent in
 * @param seasonNumber season to mark episodes sent in
 * @param episodeNumber episode number(s) to mark as sent
 */
export async function markMessageSent (imdbId: string, seasonNumber: number, episodeNumber: number): Promise<void>

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param imdbId show to mark episodes as sent for
 * @param seasonNumber season to mark episodes as sent for
 * @param episodeNumber episode number(s) to mark as sent
 */
export async function markMessageSent (imdbId: string, seasonNumber: number, episodeNumber: number | number[]): Promise<void> {
  // handle overloaded function to turn params into an array
  const episodeNumbers: number[] = (Array.isArray(episodeNumber) ? episodeNumber : [episodeNumber])

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
export async function createNewSubscription (imdbId: string, tvdbSeriesId: number, seriesName: string, channel: TextBasedChannel): Promise<Show> {
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
      imdbId,
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
export async function removeSubscription (imdbId: string, channelId: string): Promise<Show> {
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
export async function removeAllSubscriptions (id: string, idType: keyof Destination = 'channelId'): Promise<void> {
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
export async function pruneUnsubscribedShows (): Promise<void> {
  const result = await client.show.deleteMany({
    where: {
      destinations: {
        isEmpty: true
      }
    }
  })

  console.info(`Pruned shows ${result.count} with no destinations`)
}
