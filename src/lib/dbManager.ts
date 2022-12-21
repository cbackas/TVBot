import { Show } from "@prisma/client"
import moment from "moment-timezone"
import client from "./prisma"
import { getTimezone } from "./timezones"
import { getSeries } from "./tvdb"

export const updateDBEpisodes = async (show: Show): Promise<void> => {
  const series = await getSeries(show.tvdbId)

  // delete all episodes that have already aired
  const deleteStatement = client.episode.deleteMany({
    where: {
      showId: show.id,
      airDate: {
        lt: new Date()
      }
    }
  })

  const country = series.latestNetwork.country
  const timezone = getTimezone(country)

  const today = new Date()
  const getAirDate = (dateStr: string, timeStr: string | null) => {
    try {
      return moment.tz(`${dateStr} ${timeStr !== null ? timeStr : '00:00'}`, timezone)
    } catch (error) {
      throw new Error(`Could not parse air date`)
    }
  }

  const statements = series.episodes
    .filter((episode) => {
      if (episode.aired === null) return false
      return getAirDate(episode.aired, series.airsTime).toDate() > today
    })
    .map((episode) => {
      const airDate = getAirDate(episode.aired, series.airsTime)
      const airDateUTC = airDate.utc().toDate()

      return client.episode.upsert({
        where: {
          showId_season_number: {
            showId: show.id,
            season: episode.seasonNumber,
            number: episode.number
          }
        },
        update: {
          title: episode.name,
          airDate: airDateUTC
        },
        create: {
          showId: show.id,
          season: episode.seasonNumber,
          number: episode.number,
          title: episode.name,
          airDate: airDateUTC
        }
      })
    })

  const results = await client.$transaction([deleteStatement, ...statements])

  console.info(`[SYNC] ${show.name} / ${results[0].count} episodes deleted / ${statements.length} episodes stored`)
}

export async function markMessageSent(showId: string, seasonNumber: number, episodeNumbers: number[]): Promise<void>
export async function markMessageSent(showId: string, seasonNumber: number, episodeNumber: number): Promise<void>

export async function markMessageSent(showId: string, seasonNumber: number, episodeNumber: number | number[]): Promise<void> {
  let episodeNumbers: number[] = (Array.isArray(episodeNumber) ? episodeNumber : [episodeNumber])

  await client.$transaction(episodeNumbers.map((episodeNumber) => {
    return client.episode.update({
      where: {
        showId_season_number: {
          showId: showId,
          season: seasonNumber,
          number: episodeNumber
        }
      },
      data: {
        messageSent: true
      }
    })
  }))
}
