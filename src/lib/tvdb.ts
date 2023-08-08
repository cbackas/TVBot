import axios, { type AxiosRequestConfig } from 'axios'
import { type SearchByRemoteIdResult, type SearchResult, type SeriesExtendedRecord } from '../interfaces/tvdb.generated'

if (process.env.TVDB_API_KEY === undefined) throw new Error('TVDB_API_KEY is not defined')
if (process.env.TVDB_USER_PIN === undefined) throw new Error('TVDB_USER_PIN is not defined')

let token: string | undefined

async function getToken (): Promise<string> {
  if (token != null) return token

  const response = await axios.post('https://api4.thetvdb.com/v4/login', {
    apikey: process.env.TVDB_API_KEY,
    pin: process.env.TVDB_USER_PIN
  })

  token = response.data.data.token
  return response.data.data.token
}

async function axiosOptions (): Promise<AxiosRequestConfig<any>> {
  const token = await getToken()
  return {
    baseURL: 'https://api4.thetvdb.com/v4',
    headers: {
      Authorization: `Bearer ${token}`
    }
  }
}

export async function getSeriesByImdbId (imdbId: string): Promise<SeriesExtendedRecord | undefined> {
  const options = await axiosOptions()

  const response = await axios.get<{
    data?: SearchByRemoteIdResult[]
  }>(`/search/remoteid/${imdbId}`, options)

  if (response.status !== 200) {
    console.error(JSON.stringify(response.data))
    return undefined
  }

  const data = response.data.data?.at(0)
  const series = data?.series
  if (data == null || series == null || series.id == null) return undefined

  return await getSeries(series.id)
}

export async function getSeries (tvdbId: number): Promise<SeriesExtendedRecord | undefined> {
  const options = await axiosOptions()

  const response = await axios.get<{
    data?: SeriesExtendedRecord
  }>(`/series/${tvdbId}/extended`, {
    ...options,
    headers: options.headers,
    params: {
      short: true,
      meta: 'episodes'
    }
  })

  const series = response.data?.data

  return series
}

export async function getSeriesByName (query: string): Promise<SeriesExtendedRecord | undefined> {
  const options = await axiosOptions()

  const response = await axios.get<{
    data: SearchResult[]
  }>(`/search?type=series&limit=1&q=${query}`, options)

  const searchResult = response.data?.data
  if (searchResult == null || searchResult[0].tvdb_id == null) return undefined

  const series = await getSeries(parseInt(searchResult[0].tvdb_id))

  return series
}
