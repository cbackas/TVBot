export interface RootSearchRemoteID {
  data: SearchRemoteID[]
  status: string
}

export interface SearchRemoteID {
  series: Series
  people: People
  movie: Movie
  episode: Episode
  company: Company
}

export interface Series {
  aliases: Alias[]
  averageRuntime: number
  country: string
  defaultSeasonType: number
  episodes: Episode[]
  firstAired: string
  id: number
  image: string
  isOrderRandomized: boolean
  lastAired: string
  lastUpdated: string
  name: string
  nameTranslations: string[]
  nextAired: string
  originalCountry: string
  originalLanguage: string
  overviewTranslations: string[]
  score: number
  slug: string
  status: Status
  year: string
}

export interface Alias {
  language: string
  name: string
}

export interface Companies {
  studio: Studio
  network: Network
  production: Production
  distributor: Distributor
  special_effects: SpecialEffects
}

export interface Studio {
  activeDate: string
  aliases: Alias[]
  country: string
  id: number
  inactiveDate: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  primaryCompanyType: number
  slug: string
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

export interface ParentCompany {
  id: number
  name: string
  relation: Relation
}

export interface Relation {
  id: number
  typeName: string
}

export interface TagOption {
  helpText: string
  id: number
  name: string
  tag: number
  tagName: string
}

export interface Network {
  activeDate: string
  aliases: Alias[]
  country: string
  id: number
  inactiveDate: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  primaryCompanyType: number
  slug: string
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

export interface Production {
  activeDate: string
  aliases: Alias[]
  country: string
  id: number
  inactiveDate: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  primaryCompanyType: number
  slug: string
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

export interface Distributor {
  activeDate: string
  aliases: Alias[]
  country: string
  id: number
  inactiveDate: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  primaryCompanyType: number
  slug: string
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

export interface SpecialEffects {
  activeDate: string
  aliases: Alias[]
  country: string
  id: number
  inactiveDate: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  primaryCompanyType: number
  slug: string
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

export interface Type {
  alternateName: string
  id: number
  name: string
  type: string
}

export interface Status {
  id: number
  keepUpdated: boolean
  name: string
  recordType: string
}

export interface People {
  aliases: Alias[]
  id: number
  image: string
  lastUpdated: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  score: number
}

export interface Movie {
  aliases: Alias[]
  id: number
  image: string
  lastUpdated: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  score: number
  slug: string
  status: Status
  runtime: number
  year: string
}

export interface Company {
  activeDate: string
  aliases: Alias[]
  country: string
  id: number
  inactiveDate: string
  name: string
  nameTranslations: string[]
  overviewTranslations: string[]
  primaryCompanyType: number
  slug: string
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

/**
 * Series
 */

export interface RootSeries {
  status: string
  data: Series
}

export interface Series {
  id: number
  name: string
  slug: string
  image: string
  nameTranslations: string[]
  overviewTranslations: string[]
  aliases: Alias[]
  firstAired: string
  lastAired: string
  nextAired: string
  score: number
  status: Status
  originalCountry: string
  originalLanguage: string
  defaultSeasonType: number
  isOrderRandomized: boolean
  lastUpdated: string
  averageRuntime: number
  episodes: Episode[]
  overview: string
  year: string
  artworks: any
  companies: Company[]
  originalNetwork: OriginalNetwork
  latestNetwork: LatestNetwork
  genres: Genre[]
  trailers: Trailer[]
  lists: List[]
  remoteIds: RemoteId[]
  characters: any
  airsDays: AirsDays
  airsTime: string | null
  seasons: Season[]
  tags: Tag[]
  contentRatings: ContentRating[]
  seasonTypes: SeasonType[]
}

export interface Alias {
  language: string
  name: string
}

export interface Status {
  id: number
  name: string
  recordType: string
  keepUpdated: boolean
}

export interface Episode {
  id: number
  seriesId: number
  name: string
  aired: string
  runtime: number
  nameTranslations: any
  overview: string
  overviewTranslations: any
  image: string
  imageType: number
  isMovie: number
  seasons: any
  number: number
  seasonNumber: number
  lastUpdated: string
  finaleType?: string
  year: string
}

export interface CompanyType {
  companyTypeId: number
  companyTypeName: string
}

export interface OriginalNetwork {
  id: number
  name: string
  slug: string
  nameTranslations: string[]
  overviewTranslations: string[]
  aliases: any[]
  country: string
  primaryCompanyType: number
  activeDate: any
  inactiveDate: any
  companyType: CompanyType
  parentCompany: ParentCompany
  tagOptions: TagOption[]
}

export interface TagOption {
  id: number
  tag: number
  tagName: string
  name: string
  helpText: string
}

export interface LatestNetwork {
  id: number
  name: string
  slug: string
  nameTranslations: string[]
  overviewTranslations: string[]
  aliases: any[]
  country: string
  primaryCompanyType: number
  activeDate: any
  inactiveDate: any
  companyType: CompanyType3
  parentCompany: ParentCompany3
  tagOptions: TagOption2[]
}

export interface CompanyType3 {
  companyTypeId: number
  companyTypeName: string
}

export interface ParentCompany3 {
  id: any
  name: any
  relation: Relation3
}

export interface Relation3 {
  id: any
  typeName: any
}

export interface TagOption2 {
  id: number
  tag: number
  tagName: string
  name: string
  helpText: string
}

export interface Genre {
  id: number
  name: string
  slug: string
}

export interface Trailer {
  id: number
  name: string
  url: string
  language: string
  runtime: number
}

export interface List {
  id: number
  name: string
  overview: string
  url: string
  isOfficial: boolean
  nameTranslations: string[]
  overviewTranslations: string[]
  aliases: any[]
  score: number
  image: string
  imageIsFallback: boolean
  remoteIds: any
  tags: any
}

export interface RemoteId {
  id: string
  type: number
  sourceName: string
}

export interface AirsDays {
  sunday: boolean
  monday: boolean
  tuesday: boolean
  wednesday: boolean
  thursday: boolean
  friday: boolean
  saturday: boolean
}

export interface Season {
  id: number
  seriesId: number
  type: Type
  number: number
  nameTranslations: any[]
  overviewTranslations: string[]
  companies: Companies
  lastUpdated: string
  image?: string
  imageType?: number
}

export interface Tag {
  id: number
  tag: number
  tagName: string
  name: string
  helpText?: string
}

export interface ContentRating {
  id: number
  name: string
  country: string
  description: string
  contentType: string
  order: number
  fullname: any
}

export interface SeasonType {
  id: number
  name: string
  type: string
  alternateName: any
}
