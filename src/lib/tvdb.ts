import process from "node:process"
import axios, { AxiosError, type AxiosRequestConfig } from "npm:axios"
import {
  type SearchByRemoteIdResult,
  type SearchResult,
  type SeriesExtendedRecord,
} from "interfaces/tvdb.generated.ts"

if (process.env.TVDB_API_KEY === undefined) {
  throw new Error("TVDB_API_KEY is not defined")
}
if (process.env.TVDB_USER_PIN === undefined) {
  throw new Error("TVDB_USER_PIN is not defined")
}

let token: string | undefined

async function getToken(): Promise<typeof token> {
  if (token != null) return token

  try {
    const response = await axios.post("https://api4.thetvdb.com/v4/login", {
      apikey: process.env.TVDB_API_KEY,
      pin: process.env.TVDB_USER_PIN,
    })

    token = response.data.data.token
    return response.data.data.token
  } catch (error) {
    logPossibleAxiosError(error, "Getting TVDB Token")
  }
  return undefined
}

function logPossibleAxiosError(error: unknown, errorPrefix: string): void {
  if (error instanceof AxiosError) {
    console.error(`Error ${errorPrefix}:`, {
      url: error.config?.url,
      code: error.code,
      data: error.response?.data,
      status: error.response?.status,
    })
  } else {
    console.error(`Unexpected Error ${errorPrefix}:`, error)
  }
}

async function axiosOptions(): Promise<AxiosRequestConfig> {
  const token = await getToken()
  if (token == null) {
    throw new Error("Failed to get TVDB token, couldn't build axios options")
  }
  return {
    baseURL: "https://api4.thetvdb.com/v4",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  }
}

export async function getSeriesByImdbId(
  imdbId: string,
): Promise<SeriesExtendedRecord | undefined> {
  const data = await searchSeriesByImdbId(imdbId)
  if (data == null) {
    return undefined
  } else if (data?.series?.id != null) {
    return await getSeries(data?.series.id)
  } else if (data?.season?.seriesId != null) {
    return await getSeries(data?.season?.seriesId)
  }
}

export async function getSeries(
  tvdbId: number,
): Promise<SeriesExtendedRecord | undefined> {
  try {
    const options = await axiosOptions()
    const response = await axios.get<{
      data?: SeriesExtendedRecord
    }>(`/series/${tvdbId}/extended`, {
      ...options,
      headers: options.headers,
      params: {
        short: true,
        meta: "episodes",
      },
    })

    return response.data?.data
  } catch (error) {
    logPossibleAxiosError(error, "Getting Extended Show Data")
  }
  return undefined
}

async function searchSeriesByImdbId(
  imdbId: string,
): Promise<SearchByRemoteIdResult | undefined> {
  try {
    const options = await axiosOptions()
    const response = await axios.get<{
      data?: SearchByRemoteIdResult[]
    }>(`/search/remoteid/${imdbId}`, options)

    return response.data.data?.at(0)
  } catch (error) {
    logPossibleAxiosError(error, "Searching Series by IMDB ID")
  }
  return undefined
}

async function searchSeriesByName(
  query: string,
): Promise<SearchResult[] | undefined> {
  try {
    const options = await axiosOptions()
    const response = await axios.get<{
      data: SearchResult[]
    }>(`/search?type=series&limit=10&q=${query}`, options)

    const searchResult = response.data?.data
    if (searchResult == null || searchResult[0].tvdb_id == null) {
      return undefined
    }
    return searchResult
  } catch (error) {
    logPossibleAxiosError(error, "Searching Series")
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
