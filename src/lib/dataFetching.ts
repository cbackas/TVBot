import axios, { AxiosRequestConfig } from 'axios'
import { ExternalIds, ExternalSources } from '../interfaces/tmdb'

const tvdbBaseUrl = 'https://api4.thetvdb.com/v4'

const tmdbOptions: AxiosRequestConfig<any> = {
  baseURL: 'https://api.themoviedb.org/3',
  params: {
    api_key: process.env.TMDB_API_KEY
  },
  headers: { "Accept-Encoding": "gzip,deflate,compress" }
}

export const getAiringSoon = async () => {
  const paginatedData = async (page: number = 1, totalPages?: number, data: any[] = []): Promise<any[]> => {
    const response = await axios.get('/tv/on_the_air', {
      ...tmdbOptions,
      params: {
        ...tmdbOptions.params,
        page
      }
    })

    const { results, page: currentPage, total_pages } = response.data

    if (!Array.isArray(results)) throw new Error('Results is not an array')

    const combinedData = [...data, ...results]

    const total = totalPages ?? total_pages

    if (currentPage < total) {
      const nextPage = await paginatedData(currentPage + 1, total, combinedData)
      return nextPage
    }
    return combinedData
  }

  return await paginatedData()
}

export const getExternalIds = async (tmdbId: number) => {
  const response = await axios.get<ExternalIds>(`/tv/${tmdbId}/external_ids`, tmdbOptions)

  return response.data
}

export const findByExternalId = async (externalId: string, externalSource: ExternalSources) => {
  const response = await axios.get(`/find/${externalId}`, {
    ...tmdbOptions,
    params: {
      ...tmdbOptions.params,
      external_source: externalSource
    }
  })

  return response.data
}