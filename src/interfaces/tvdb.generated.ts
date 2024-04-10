/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

/** An alias model, which can be associated with a series, season, movie, person, or list. */
export interface Alias {
  /**
   * A 3-4 character string indicating the language of the alias, as defined in Language.
   * @max 4
   */
  language?: string;
  /**
   * A string containing the alias itself.
   * @max 100
   */
  name?: string;
}

/** base artwork record */
export interface ArtworkBaseRecord {
  /** @format int64 */
  height?: number;
  id?: number;
  image?: string;
  language?: string;
  score?: number;
  thumbnail?: string;
  /**
   * The artwork type corresponds to the ids from the /artwork/types endpoint.
   * @format int64
   */
  type?: number;
  /** @format int64 */
  width?: number;
}

/** extended artwork record */
export interface ArtworkExtendedRecord {
  episodeId?: number;
  /** @format int64 */
  height?: number;
  /** @format int64 */
  id?: number;
  image?: string;
  language?: string;
  movieId?: number;
  networkId?: number;
  peopleId?: number;
  score?: number;
  seasonId?: number;
  seriesId?: number;
  seriesPeopleId?: number;
  /** artwork status record */
  status?: ArtworkStatus;
  tagOptions?: TagOption[];
  thumbnail?: string;
  /** @format int64 */
  thumbnailHeight?: number;
  /** @format int64 */
  thumbnailWidth?: number;
  /**
   * The artwork type corresponds to the ids from the /artwork/types endpoint.
   * @format int64
   */
  type?: number;
  /** @format int64 */
  updatedAt?: number;
  /** @format int64 */
  width?: number;
}

/** artwork status record */
export interface ArtworkStatus {
  /** @format int64 */
  id?: number;
  name?: string;
}

/** artwork type record */
export interface ArtworkType {
  /** @format int64 */
  height?: number;
  /** @format int64 */
  id?: number;
  imageFormat?: string;
  name?: string;
  recordType?: string;
  slug?: string;
  /** @format int64 */
  thumbHeight?: number;
  /** @format int64 */
  thumbWidth?: number;
  /** @format int64 */
  width?: number;
}

/** base award record */
export interface AwardBaseRecord {
  id?: number;
  name?: string;
}

/** base award category record */
export interface AwardCategoryBaseRecord {
  allowCoNominees?: boolean;
  /** base award record */
  award?: AwardBaseRecord;
  forMovies?: boolean;
  forSeries?: boolean;
  /** @format int64 */
  id?: number;
  name?: string;
}

/** extended award category record */
export interface AwardCategoryExtendedRecord {
  allowCoNominees?: boolean;
  /** base award record */
  award?: AwardBaseRecord;
  forMovies?: boolean;
  forSeries?: boolean;
  /** @format int64 */
  id?: number;
  name?: string;
  nominees?: AwardNomineeBaseRecord[];
}

/** extended award record */
export interface AwardExtendedRecord {
  categories?: AwardCategoryBaseRecord[];
  id?: number;
  name?: string;
  /** @format int64 */
  score?: number;
}

/** base award nominee record */
export interface AwardNomineeBaseRecord {
  /** character record */
  character?: Character;
  details?: string;
  /** base episode record */
  episode?: EpisodeBaseRecord;
  /** @format int64 */
  id?: number;
  isWinner?: boolean;
  /** base movie record */
  movie?: MovieBaseRecord;
  /** The base record for a series. All series airs time like firstAired, lastAired, nextAired, etc. are in US EST for US series, and for all non-US series, the time of the show’s country capital or most populous city. For streaming services, is the official release time. See https://support.thetvdb.com/kb/faq.php?id=29. */
  series?: SeriesBaseRecord;
  year?: string;
  category?: string;
  name?: string;
}

/** biography record */
export interface Biography {
  biography?: string;
  language?: string;
}

/** character record */
export interface Character {
  aliases?: Alias[];
  /** base record info */
  episode?: RecordInfo;
  episodeId?: number;
  /** @format int64 */
  id?: number;
  image?: string;
  isFeatured?: boolean;
  movieId?: number;
  /** base record info */
  movie?: RecordInfo;
  name?: string;
  nameTranslations?: string[];
  overviewTranslations?: string[];
  peopleId?: number;
  personImgURL?: string;
  peopleType?: string;
  seriesId?: number;
  /** base record info */
  series?: RecordInfo;
  /** @format int64 */
  sort?: number;
  tagOptions?: TagOption[];
  /** @format int64 */
  type?: number;
  url?: string;
  personName?: string;
}

/** A company record */
export type Company = {
  activeDate?: string;
  aliases?: Alias[];
  country?: string;
  /** @format int64 */
  id?: number;
  inactiveDate?: string;
  name?: string;
  nameTranslations?: string[];
  overviewTranslations?: string[];
  /** @format int64 */
  primaryCompanyType?: number;
  slug?: string;
  /** A parent company record */
  parentCompany?: ParentCompany;
  tagOptions?: TagOption[];
}

/** A parent company record */
export interface ParentCompany {
  id?: number;
  name?: string;
  /** A company relationship */
  relation?: CompanyRelationShip;
}

/** A company relationship */
export interface CompanyRelationShip {
  id?: number;
  typeName?: string;
}

/** A company type record */
export interface CompanyType {
  companyTypeId?: number;
  companyTypeName?: string;
}

/** content rating record */
export interface ContentRating {
  /** @format int64 */
  id?: number;
  name?: string;
  description?: string;
  country?: string;
  contentType?: string;
  order?: number;
  fullName?: string;
}

/** country record */
export interface Country {
  id?: string;
  name?: string;
  shortCode?: string;
}

/** Entity record */
export interface Entity {
  movieId?: number;
  /** @format int64 */
  order?: number;
  seriesId?: number;
}

/** Entity Type record */
export interface EntityType {
  id?: number;
  name?: string;
  hasSpecials?: boolean;
}

/** entity update record */
export interface EntityUpdate {
  entityType?: string;
  method?: string;
  /** @format int64 */
  recordId?: number;
  /** @format int64 */
  timeStamp?: number;
  /**
   * Only present for episodes records
   * @format int64
   */
  seriesId?: number;
  /** @format int64 */
  mergeToId?: number;
  mergeToEntityType?: string;
}

/** base episode record */
export interface EpisodeBaseRecord {
  aired?: string;
  airsAfterSeason?: number;
  airsBeforeEpisode?: number;
  airsBeforeSeason?: number;
  /** season, midseason, or series */
  finaleType?: string;
  /** @format int64 */
  id?: number;
  image?: string;
  imageType?: number;
  /** @format int64 */
  isMovie?: number;
  lastUpdated?: string;
  linkedMovie?: number;
  name?: string;
  nameTranslations?: string[];
  number: number;
  overview?: string;
  overviewTranslations?: string[];
  runtime?: number;
  seasonNumber: number;
  seasons?: SeasonBaseRecord[];
  /** @format int64 */
  seriesId?: number;
  seasonName?: string;
  year?: string;
}

/** extended episode record */
export interface EpisodeExtendedRecord {
  aired?: string;
  airsAfterSeason?: number;
  airsBeforeEpisode?: number;
  airsBeforeSeason?: number;
  awards?: AwardBaseRecord[];
  characters?: Character[];
  companies?: Company[];
  contentRatings?: ContentRating[];
  /** season, midseason, or series */
  finaleType?: string;
  /** @format int64 */
  id?: number;
  image?: string;
  imageType?: number;
  /** @format int64 */
  isMovie?: number;
  lastUpdated?: string;
  linkedMovie?: number;
  name?: string;
  nameTranslations?: string[];
  networks?: Company[];
  nominations?: AwardNomineeBaseRecord[];
  number?: number;
  overview?: string;
  overviewTranslations?: string[];
  productionCode?: string;
  remoteIds?: RemoteID[];
  runtime?: number;
  seasonNumber?: number;
  seasons?: SeasonBaseRecord[];
  /** @format int64 */
  seriesId?: number;
  studios?: Company[];
  tagOptions?: TagOption[];
  trailers?: Trailer[];
  /** translation extended record */
  translations?: TranslationExtended;
  year?: string;
}

/** User favorites record */
export interface Favorites {
  series?: number[];
  movies?: number[];
  episodes?: number[];
  artwork?: number[];
  people?: number[];
  lists?: number[];
}

/** Favorites record */
export interface FavoriteRecord {
  series?: number;
  movies?: number;
  episodes?: number;
  artwork?: number;
  people?: number;
  list?: number;
}

/** gender record */
export interface Gender {
  /** @format int64 */
  id?: number;
  name?: string;
}

/** base genre record */
export interface GenreBaseRecord {
  /** @format int64 */
  id?: number;
  name?: string;
  slug?: string;
}

/** language record */
export interface Language {
  id?: string;
  name?: string;
  nativeName?: string;
  shortCode?: string;
}

/** base list record */
export interface ListBaseRecord {
  aliases?: Alias[];
  /** @format int64 */
  id?: number;
  image?: string;
  imageIsFallback?: boolean;
  isOfficial?: boolean;
  name?: string;
  nameTranslations?: string[];
  overview?: string;
  overviewTranslations?: string[];
  remoteIds?: RemoteID[];
  tags?: TagOption[];
  score?: number;
  url?: string;
}

/** extended list record */
export interface ListExtendedRecord {
  aliases?: Alias[];
  entities?: Entity[];
  /** @format int64 */
  id?: number;
  image?: string;
  imageIsFallback?: boolean;
  isOfficial?: boolean;
  name?: string;
  nameTranslations?: string[];
  overview?: string;
  overviewTranslations?: string[];
  /** @format int64 */
  score?: number;
  url?: string;
}

/** base movie record */
export interface MovieBaseRecord {
  aliases?: Alias[];
  /** @format int64 */
  id?: number;
  image?: string;
  lastUpdated?: string;
  name?: string;
  nameTranslations?: string[];
  overviewTranslations?: string[];
  /** @format double */
  score?: number;
  slug?: string;
  /** status record */
  status?: Status;
  runtime?: number;
  year?: string;
}

/** extended movie record */
export interface MovieExtendedRecord {
  aliases?: Alias[];
  artworks?: ArtworkBaseRecord[];
  audioLanguages?: string[];
  awards?: AwardBaseRecord[];
  boxOffice?: string;
  boxOfficeUS?: string;
  budget?: string;
  characters?: Character[];
  /** Companies by type record */
  companies?: Companies;
  contentRatings?: ContentRating[];
  /** release record */
  first_release?: Release;
  genres?: GenreBaseRecord[];
  /** @format int64 */
  id?: number;
  image?: string;
  inspirations?: Inspiration[];
  lastUpdated?: string;
  lists?: ListBaseRecord[];
  name?: string;
  nameTranslations?: string[];
  originalCountry?: string;
  originalLanguage?: string;
  overviewTranslations?: string[];
  production_countries?: ProductionCountry[];
  releases?: Release[];
  remoteIds?: RemoteID[];
  runtime?: number;
  /** @format double */
  score?: number;
  slug?: string;
  spoken_languages?: string[];
  /** status record */
  status?: Status;
  studios?: StudioBaseRecord[];
  subtitleLanguages?: string[];
  tagOptions?: TagOption[];
  trailers?: Trailer[];
  /** translation extended record */
  translations?: TranslationExtended;
  year?: string;
}

/** base people record */
export interface PeopleBaseRecord {
  aliases?: Alias[];
  /** @format int64 */
  id?: number;
  image?: string;
  lastUpdated?: string;
  name?: string;
  nameTranslations?: string[];
  overviewTranslations?: string[];
  /** @format int64 */
  score?: number;
}

/** extended people record */
export interface PeopleExtendedRecord {
  aliases?: Alias[];
  awards?: AwardBaseRecord[];
  biographies?: Biography[];
  birth?: string;
  birthPlace?: string;
  characters?: Character[];
  death?: string;
  gender?: number;
  /** @format int64 */
  id?: number;
  image?: string;
  lastUpdated?: string;
  name?: string;
  nameTranslations?: string[];
  overviewTranslations?: string[];
  races?: Race[];
  remoteIds?: RemoteID[];
  /** @format int64 */
  score?: number;
  slug?: string;
  tagOptions?: TagOption[];
  /** translation extended record */
  translations?: TranslationExtended;
}

/** people type record */
export interface PeopleType {
  /** @format int64 */
  id?: number;
  name?: string;
}

/** race record */
export type Race = object;

/** base record info */
export interface RecordInfo {
  image?: string;
  name?: string;
  year?: string;
}

/** release record */
export interface Release {
  country?: string;
  date?: string;
  detail?: string;
}

/** remote id record */
export interface RemoteID {
  id?: string;
  /** @format int64 */
  type?: number;
  sourceName?: string;
}

/** search result */
export interface SearchResult {
  aliases?: string[];
  companies?: string[];
  companyType?: string;
  country?: string;
  director?: string;
  first_air_time?: string;
  genres?: string[];
  id?: string;
  image_url?: string;
  name?: string;
  is_official?: boolean;
  name_translated?: string;
  network?: string;
  objectID?: string;
  officialList?: string;
  overview?: string;
  overviews?: TranslationSimple[];
  overview_translated?: string[];
  poster?: string;
  posters?: string[];
  primary_language?: string;
  remote_ids?: RemoteID[];
  status?: string;
  slug?: string;
  studios?: string[];
  title?: string;
  thumbnail?: string;
  translations?: TranslationSimple[];
  translationsWithLang?: string[];
  tvdb_id?: string;
  type?: string;
  year?: string;
}

/** search by remote reuslt is a base record for a movie, series, people, season or company search result */
export interface SearchByRemoteIdResult {
  /** The base record for a series. All series airs time like firstAired, lastAired, nextAired, etc. are in US EST for US series, and for all non-US series, the time of the show’s country capital or most populous city. For streaming services, is the official release time. See https://support.thetvdb.com/kb/faq.php?id=29. */
  series?: SeriesBaseRecord;
  /** base people record */
  people?: PeopleBaseRecord;
  /** base movie record */
  movie?: MovieBaseRecord;
  /** base episode record */
  episode?: EpisodeBaseRecord;
  /** A company record */
  company?: Company;
  /** season base record */
  season?: SeasonBaseRecord;
}

/** season genre record */
export interface SeasonBaseRecord {
  id?: number;
  image?: string;
  imageType?: number;
  lastUpdated?: string;
  name?: string;
  nameTranslations?: string[];
  /** @format int64 */
  number?: number;
  overviewTranslations?: string[];
  /** Companies by type record */
  companies?: Companies;
  /** @format int64 */
  seriesId?: number;
  /** season type record */
  type?: SeasonType;
  year?: string;
}

/** extended season record */
export interface SeasonExtendedRecord {
  artwork?: ArtworkBaseRecord[];
  /** Companies by type record */
  companies?: Companies;
  episodes?: EpisodeBaseRecord[];
  id?: number;
  image?: string;
  imageType?: number;
  lastUpdated?: string;
  name?: string;
  nameTranslations?: string[];
  /** @format int64 */
  number?: number;
  overviewTranslations?: string[];
  /** @format int64 */
  seriesId?: number;
  trailers?: Trailer[];
  /** season type record */
  type?: SeasonType;
  tagOptions?: TagOption[];
  translations?: Translation[];
  year?: string;
}

/** season type record */
export interface SeasonType {
  alternateName?: string;
  /** @format int64 */
  id?: number;
  name?: string;
  type?: string;
}

/** A series airs day record */
export interface SeriesAirsDays {
  friday?: boolean;
  monday?: boolean;
  saturday?: boolean;
  sunday?: boolean;
  thursday?: boolean;
  tuesday?: boolean;
  wednesday?: boolean;
}

/** The base record for a series. All series airs time like firstAired, lastAired, nextAired, etc. are in US EST for US series, and for all non-US series, the time of the show’s country capital or most populous city. For streaming services, is the official release time. See https://support.thetvdb.com/kb/faq.php?id=29. */
export interface SeriesBaseRecord {
  aliases?: Alias[];
  averageRuntime?: number;
  country?: string;
  /** @format int64 */
  defaultSeasonType?: number;
  episodes?: EpisodeBaseRecord[];
  firstAired?: string;
  id?: number;
  image?: string;
  isOrderRandomized?: boolean;
  lastAired?: string;
  lastUpdated?: string;
  name?: string;
  nameTranslations?: string[];
  nextAired?: string;
  originalCountry?: string;
  originalLanguage?: string;
  overviewTranslations?: string[];
  /** @format double */
  score?: number;
  slug?: string;
  /** status record */
  status?: Status;
  year?: string;
}

/** The extended record for a series. All series airs time like firstAired, lastAired, nextAired, etc. are in US EST for US series, and for all non-US series, the time of the show’s country capital or most populous city. For streaming services, is the official release time. See https://support.thetvdb.com/kb/faq.php?id=29. */
export type SeriesExtendedRecord = {
  abbreviation?: string;
  /** A series airs day record */
  airsDays?: SeriesAirsDays;
  airsTime: string;
  aliases?: Alias[];
  artworks?: ArtworkExtendedRecord[];
  averageRuntime?: number;
  characters?: Character[];
  contentRatings?: ContentRating[];
  country?: string;
  /** @format int64 */
  defaultSeasonType?: number;
  episodes: EpisodeBaseRecord[];
  firstAired?: string;
  lists?: ListBaseRecord[];
  genres: GenreBaseRecord[];
  id: number;
  image: string;
  isOrderRandomized?: boolean;
  lastAired?: string;
  lastUpdated?: string;
  name: string;
  nameTranslations?: string[];
  companies?: Company[];
  nextAired?: string;
  originalCountry?: string;
  originalLanguage?: string;
  /** A company record */
  originalNetwork?: Company;
  overview?: string;
  /** A company record */
  latestNetwork?: Company;
  overviewTranslations?: string[];
  remoteIds: RemoteID[];
  /** @format double */
  score?: number;
  seasons: SeasonBaseRecord[];
  seasonTypes?: SeasonType[];
  slug?: string;
  /** status record */
  status: Status;
  tags?: TagOption[];
  trailers?: Trailer[];
  /** translation extended record */
  translations?: TranslationExtended;
  year?: string;
}

/** source type record */
export interface SourceType {
  /** @format int64 */
  id?: number;
  name?: string;
  postfix?: string;
  prefix?: string;
  slug?: string;
  /** @format int64 */
  sort?: number;
}

/** status record */
export type Status = Required<{
  /** @format int64 */
  id?: number;
  keepUpdated?: boolean;
  name?: string;
  recordType?: string;
}>

/** studio record */
export interface StudioBaseRecord {
  /** @format int64 */
  id?: number;
  name?: string;
  parentStudio?: number;
}

/** tag record */
export interface Tag {
  allowsMultiple?: boolean;
  helpText?: string;
  /** @format int64 */
  id?: number;
  name?: string;
  options?: TagOption[];
}

/** tag option record */
export interface TagOption {
  helpText?: string;
  /** @format int64 */
  id?: number;
  name?: string;
  /** @format int64 */
  tag?: number;
  tagName?: string;
}

/** trailer record */
export interface Trailer {
  /** @format int64 */
  id?: number;
  language?: string;
  name?: string;
  url?: string;
  runtime?: number;
}

/** translation record */
export interface Translation {
  aliases?: string[];
  isAlias?: boolean;
  isPrimary?: boolean;
  language?: string;
  name?: string;
  overview?: string;
  /** Only populated for movie translations.  We disallow taglines without a title. */
  tagline?: string;
}

/** translation simple record */
export interface TranslationSimple {
  language?: string;
}

/** translation extended record */
export interface TranslationExtended {
  nameTranslations?: Translation[];
  overviewTranslations?: Translation[];
  alias?: string[];
}

/** a entity with selected tag option */
export interface TagOptionEntity {
  name?: string;
  tagName?: string;
  tagId?: number;
}

/** User info record */
export interface UserInfo {
  id?: number;
  language?: string;
  name?: string;
  type?: string;
}

/** Movie inspiration record */
export interface Inspiration {
  /** @format int64 */
  id?: number;
  type?: string;
  type_name?: string;
  url?: string;
}

/** Movie inspiration type record */
export interface InspirationType {
  /** @format int64 */
  id?: number;
  name?: string;
  description?: string;
  reference_name?: string;
  url?: string;
}

/** Production country record */
export interface ProductionCountry {
  /** @format int64 */
  id?: number;
  country?: string;
  name?: string;
}

/** Companies by type record */
export interface Companies {
  /** A company record */
  studio?: Company;
  /** A company record */
  network?: Company;
  /** A company record */
  production?: Company;
  /** A company record */
  distributor?: Company;
  /** A company record */
  special_effects?: Company;
}

/** Links for next, previous and current record */
export interface Links {
  prev?: string;
  self?: string;
  next?: string;
  total_items?: number;
  page_size?: number;
}
