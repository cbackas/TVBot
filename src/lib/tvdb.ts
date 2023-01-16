import axios, { AxiosRequestConfig } from "axios"
import { RootSearchRemoteID, RootSeries, Series } from "../interfaces/tvdb"

if (process.env.TVDB_API_KEY === undefined) throw new Error('TVDB_API_KEY is not defined')
if (process.env.TVDB_USER_PIN === undefined) throw new Error('TVDB_USER_PIN is not defined')

let token: string | undefined

const getToken = async (): Promise<string> => {
  if (token) return token

  const response = await axios.post('https://api4.thetvdb.com/v4/login', {
    apikey: process.env.TVDB_API_KEY,
    pin: process.env.TVDB_USER_PIN,
  })

  token = response.data.data.token
  return response.data.data.token
}


const axiosOptions = async (): Promise<AxiosRequestConfig<any>> => {
  const token = await getToken()
  return {
    baseURL: 'https://api4.thetvdb.com/v4',
    headers: {
      Authorization: `Bearer ${token}`
    }
  }
}

export const getSeriesByImdbId = async (imdbId: string): Promise<Series | undefined> => {
  const options = await axiosOptions()

  const response = await axios.get<RootSearchRemoteID>(`/search/remoteid/${imdbId}`, options)

  const data = response.data.data[0]
  const series = data?.series
  if (!data || !series) return undefined

  return await getSeries(series.id)
}

export const getSeries = async (tvdbId: number): Promise<Series> => {
  const options = await axiosOptions()

  const series = await axios.get<RootSeries>(`/series/${tvdbId}/extended`, {
    ...options,
    headers: options.headers,
    params: {
      short: true,
      meta: 'episodes'
    }
  })

  return series.data.data
}
