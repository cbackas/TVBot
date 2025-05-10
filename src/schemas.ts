import * as z from "npm:zod"

export const zDestination = z.object({
  channelId: z.string(),
  forumId: z.string().optional(),
})
export type Destination = z.infer<typeof zDestination>

export const zEpisode = z.object({
  season: z.number(),
  nuumber: z.number(),
  title: z.string(),
  airDate: z.iso.datetime(),
  messageSent: z.boolean(),
})
export type Episode = z.infer<typeof zEpisode>

export const zShow = z.object({
  name: z.string(),
  imdbId: z.string(),
  tvdbId: z.string(),
  episodes: z.array(zEpisode),
  destinations: z.array(zDestination),
})
export type Show = z.infer<typeof zShow>

// export const zSettings = z.object({
//   defaultForum: z.string().nullable(),
//   allEpisodes: z.array(zDestination),
//   morningSumarryDestinations: z.array(zDestination),
// })
// export type Settings = z.infer<typeof zSettings>
