export type ExternalIds = { id: string } & {
  [key in ExternalSourcesString]: string | null
} & {
    [key in ExternalSourcesNumbers]: number | null
  }

export type ExternalSources = `${ExternalSourcesString | ExternalSourcesNumbers}`

type ExternalSourcesString = 'imdb_id' | 'freebase_mid' | 'freebase_id' | 'facebook_id' | 'instagram_id' | 'twitter_id'
type ExternalSourcesNumbers = 'tvdb_id' | 'tvrage_id'
