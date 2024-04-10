import { type Destination } from '@prisma/client'
import { type APIEmbed, type APIEmbedField } from 'discord.js'
import { type SeriesExtendedRecord } from '../interfaces/tvdb.generated'

export function buildShowEmbed (imdbId: string, tvdbSeries: SeriesExtendedRecord, destinations: Destination[] = []): APIEmbed {
  const country = tvdbSeries.latestNetwork.country ?? 'usa'

  // put together some basic data fields
  const fields: APIEmbedField[] = [
    {
      name: 'Network',
      value: `${tvdbSeries.latestNetwork.name ?? 'unknown'} (${country.toUpperCase()})`,
      inline: true
    },
    {
      name: 'Status',
      value: tvdbSeries.status.name,
      inline: true
    },
    {
      name: 'Seasons',
      value: tvdbSeries.seasons.length.toFixed(0),
      inline: true
    },
    {
      name: 'Genres',
      value: tvdbSeries.genres.map(g => g.name).join(', '),
      inline: true
    }
  ]

  // add the linked channels field if there are any
  if (destinations.length > 0) {
    fields.push({
      name: 'Linked Channels',
      value: destinations.map(d => `<#${d.channelId}>`).join('\n'),
      inline: true
    })
  }

  // add the links field
  fields.push({
    name: 'Links',
    value: getLinks(tvdbSeries.remoteIds).join('\n'),
    inline: true
  })

  // build and return the final embed object
  return {
    title: tvdbSeries.name,
    url: `https://www.imdb.com/title/${imdbId}`,
    thumbnail: {
      url: tvdbSeries.image
    },
    description: tvdbSeries.overview,
    fields,
    footer: {
      text: 'Powered by TVDB',
      icon_url: 'https://www.thetvdb.com/images/logo.png'
    }
  }
}

function getLinks (remoteIds: SeriesExtendedRecord['remoteIds']): string[] {
  const links: string[] = []

  for (const remote of remoteIds) {
    if (remote.id == null) continue

    if (remote.type === 2) links.push(`[IMDB](https://www.imdb.com/title/${remote.id})`)
    if (remote.type === 4) links.push(`[Official Site](${remote.id})`)
    if (remote.type === 24) links.push(`[Wikipedia](https://en.wikipedia.org/wiki/${remote.id})`)
    if (remote.type === 19) links.push(`[TV Maze](https://www.tvmaze.com/shows/${remote.id})`)
    if (remote.type === 9) links.push(`[Instagram](https://www.instagram.com/${remote.id})`)
    if (remote.type === 6) links.push(`[Twitter](https://twitter.com/${remote.id})`)
  }

  return links
}
