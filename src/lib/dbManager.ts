import { Episode, Prisma, PrismaPromise, Show } from "@prisma/client"
import client from "./prisma"
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

  if (series.airsTime === null) return

  const statements = series.episodes
    .filter((episode) => {
      const airDate = new Date(episode.aired + 'T' + series.airsTime + ':00Z')
      return airDate > new Date()
    })
    .map((episode) => {
      const airDate = new Date(episode.aired + 'T' + series.airsTime + ':00Z')

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
          airDate: airDate
        },
        create: {
          showId: show.id,
          season: episode.seasonNumber,
          number: episode.number,
          title: episode.name,
          airDate: airDate
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
