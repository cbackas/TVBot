import {
  type SearchByRemoteIdResult,
  type SearchResult,
  type SeriesExtendedRecord,
} from "interfaces/tvdb.generated.ts"
import { getEnv } from "lib/env.ts"

const baseURL = "https://api4.thetvdb.com/v4" as const

let token: string | undefined

async function getAuthToken(): Promise<{ Authorization: string }> {
  if (token == null) {
    const response = await fetch(`${baseURL}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        apikey: getEnv("TVDB_API_KEY"),
        pin: getEnv("TVDB_USER_PIN"),
      }),
    })

    if (!response.ok) {
      throw new Error("Failed to get TVDB token", {
        cause: await response.text(),
      })
    }

    const data = await response.json()
    if (!data?.data?.token) {
      throw new Error("Invalid API response: Missing token")
    }
    token = data.data.token
  }

  return { Authorization: `Bearer ${token}` }
}

export async function getSeriesByImdbId(
  imdbId: string,
): Promise<SeriesExtendedRecord | undefined> {
  const data = await searchSeriesByImdbId(imdbId)

  if (data == null) {
    return undefined
  }

  let series: SeriesExtendedRecord | undefined
  if (data?.series?.id != null) {
    series = await getSeries(data?.series.id)
  } else if (data?.season?.seriesId != null) {
    series = await getSeries(data?.season?.seriesId)
  }

  if (
    series?.originalLanguage == null || series.originalLanguage === "eng"
  ) return series

  return translateSeries(series)
}

function translateSeries(series: SeriesExtendedRecord): SeriesExtendedRecord {
  if (series.translations == null) {
    console.warn(
      `Series ${series.id} has a non-english original language but no translations`,
    )
    return series
  }

  const { nameTranslations, overviewTranslations } = series.translations
  const englishName = nameTranslations?.find((t) => t.language === "eng")?.name
  if (englishName != null) {
    series.name = englishName
  } else {
    console.warn(
      `Series ${series.id} has a non-english original language but no english name translation`,
    )
  }
  const englishOverview = overviewTranslations?.find((t) =>
    t.language === "eng"
  )?.overview
  if (englishOverview != null) {
    series.overview = englishOverview
  } else {
    console.warn(
      `Series ${series.id} has a non-english original language but no english overview translation`,
    )
  }

  return series
}

export async function getSeries(
  tvdbId: number,
): Promise<SeriesExtendedRecord | undefined> {
  try {
    const params = new URLSearchParams({
      short: "true",
      meta: "episodes,translations",
    })
    const response = await fetch(
      `${baseURL}/series/${tvdbId}/extended?${params}`,
      {
        headers: await getAuthToken(),
      },
    )

    if (!response.ok) {
      console.error(`Error Getting Extended Show Data:`, {
        url: response.url,
        status: response.status,
        data: await response.text(),
      })
      return undefined
    }

    const data: { data?: SeriesExtendedRecord } = await response.json()
    return data.data
  } catch (error) {
    console.error(`Unexpected Error Getting Extended Show Data:`, error)
  }
  return undefined
}

async function searchSeriesByImdbId(
  imdbId: string,
): Promise<SearchByRemoteIdResult | undefined> {
  try {
    const response = await fetch(
      `${baseURL}/search/remoteid/${imdbId}`,
      {
        headers: await getAuthToken(),
      },
    )

    if (!response.ok) {
      console.error(`Error Searching Series by IMDB ID:`, {
        url: response.url,
        status: response.status,
        data: await response.text(),
      })
      return undefined
    }

    const data: { data?: SearchByRemoteIdResult[] } = await response.json()
    return data.data?.at(0)
  } catch (error) {
    console.error(`Unexpected Error Searching Series by IMDB ID:`, error)
  }
  return undefined
}

async function searchSeriesByName(
  query: string,
): Promise<SearchResult[] | undefined> {
  try {
    const params = new URLSearchParams({
      type: "series",
      limit: "10",
      q: query,
    })
    const response = await fetch(
      `${baseURL}/search?${params}`,
      {
        headers: await getAuthToken(),
      },
    )

    if (!response.ok) {
      console.error(`Error Searching Series:`, {
        url: response.url,
        status: response.status,
        data: await response.text(),
      })
      return undefined
    }

    const data: { data: SearchResult[] } = await response.json()
    const searchResult = data?.data
    if (searchResult == null || searchResult[0].tvdb_id == null) {
      return undefined
    }
    return searchResult
  } catch (error) {
    console.error(`Unexpected Error Searching Series:`, error)
  }
  return undefined
}

export async function getSeriesByName(
  query: string,
): Promise<SeriesExtendedRecord | undefined> {
  const searchResult = await searchSeriesByName(query)
  if (searchResult == null || searchResult[0].tvdb_id == null) return undefined
  const series = await getSeries(parseInt(searchResult[0].tvdb_id))
  return series
}
