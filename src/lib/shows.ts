import { and, asc, eq, inArray, like, notInArray, or } from "drizzle-orm";
import moment, { type Moment } from "moment-timezone";
import { getDb } from "../database/db.js";
import { episodes, showDestinations, shows } from "../database/schema.js";
import {
  type Destination,
  type Episode,
  type Show,
  showSchema,
} from "../database/types.js";
import type {
  EpisodeBaseRecord,
  SeriesExtendedRecord,
} from "../interfaces/tvdb.generated.js";
import { getTimezone } from "./timezones.js";
import { getSeries } from "./tvdb.js";

export type { Destination, Episode, Show };

/**
 * Get a moment airdate with the specified date and time
 * @param dateStr date as string to use for the air date
 * @param timeStr time as a string to use for the airdate
 * @param timezone timezone to use for the airdate
 * @returns moment object representing the airdate
 */
function getAirDate(
  dateStr: string,
  timeStr: string | null,
  timezone: string,
): Moment {
  try {
    return moment.tz(
      `${dateStr} ${timeStr !== null ? timeStr : "00:00"}`,
      timezone,
    );
  } catch (error) {
    throw new Error(
      "Could not parse air date",
      error instanceof Error ? { cause: error } : undefined,
    );
  }
}

// ---- Query helpers ----

export async function getShowByImdbId(imdbId: string): Promise<Show | null> {
  const db = getDb();

  const row = await db.query.shows.findFirst({
    where: { imdbId },
    with: { episodes: true, destinations: true },
  });

  if (row == null) return null;
  return showSchema.parse(row);
}

export async function getAllShows(): Promise<Show[]> {
  const db = getDb();

  const rows = await db.query.shows.findMany({
    with: { episodes: true, destinations: true },
  });

  return rows.map((r) => showSchema.parse(r));
}

export async function getShowsWithUnsentEpisodes(): Promise<Show[]> {
  const db = getDb();

  const rows = await db.query.shows.findMany({
    where: { episodes: { messageSent: false } },
    with: { episodes: true, destinations: true },
  });

  return rows.map((r) => showSchema.parse(r));
}

export async function getShowsByChannelId(channelId: string): Promise<Show[]> {
  const db = getDb();

  const rows = await db.query.shows.findMany({
    where: { destinations: { channelId } },
    with: { episodes: true, destinations: true },
  });

  return rows.map((r) => showSchema.parse(r));
}

export async function searchShows(
  query: string,
  limit: number = 25,
): Promise<Pick<Show, "name" | "imdbId">[]> {
  const db = getDb();
  const condition = query.toLowerCase().startsWith("tt")
    ? or(like(shows.imdbId, `${query}%`), like(shows.name, `${query}%`))
    : like(shows.name, `${query}%`);
  return db
    .select({ name: shows.name, imdbId: shows.imdbId })
    .from(shows)
    .where(condition)
    .orderBy(asc(shows.name))
    .limit(limit);
}

/**
 * Fetch new episodes for a show and save them in the DB
 * @param imdbId imdbid of the show to update
 * @param tvdbId tvdbId of the show to update
 * @param providedSeries optional series data to use instead of fetching it
 */
export async function updateEpisodes(
  imdbId: string,
  tvdbId: number,
  providedSeries?: SeriesExtendedRecord,
): Promise<void> {
  const series: SeriesExtendedRecord | undefined =
    providedSeries ?? (await getSeries(tvdbId));

  if (series == null) {
    throw new Error(`Could not fetch series data for ${imdbId}`);
  }

  const timezone = getTimezone(series.latestNetwork?.country ?? "usa");
  const airsTime = series.airsTime;

  const upcomingEpisodes = series.episodes
    .filter((e: EpisodeBaseRecord) => {
      if (e.aired == null) return false;
      return getAirDate(e.aired, airsTime, timezone).toDate() > new Date();
    })
    .map((e) => {
      if (e.aired == null) throw new Error("Episode has no air date");

      const airDate = getAirDate(e.aired, airsTime, timezone);
      const airDateUTC = airDate.utc().toDate();
      return {
        season: e.seasonNumber,
        number: e.number,
        title: e.name ?? "",
        airDate: airDateUTC,
      };
    });

  // Upsert show
  const db = getDb();
  await db
    .insert(shows)
    .values({ imdbId, tvdbId, name: series.name })
    .onConflictDoUpdate({
      target: shows.imdbId,
      set: { tvdbId, name: series.name },
    });

  // Replace episodes
  const showRow = await db
    .select({ id: shows.id })
    .from(shows)
    .where(eq(shows.imdbId, imdbId))
    .get();

  if (showRow != null) {
    const insertValues = upcomingEpisodes.map((e) => ({
      showId: showRow.id,
      season: e.season,
      number: e.number,
      title: e.title,
      airDate: e.airDate.toISOString(),
    }));

    if (insertValues.length === 0) {
      await db.delete(episodes).where(eq(episodes.showId, showRow.id));
    } else {
      await db.batch([
        db.delete(episodes).where(eq(episodes.showId, showRow.id)),
        db.insert(episodes).values(insertValues),
      ]);
    }
  }

  console.info(
    `[Get Episode Data] ${series.name} / Upcoming episodes: ${upcomingEpisodes.length}`,
  );
}

/**
 * Updates all shows in the DB with new episodes
 */
export async function checkForAiringEpisodes(): Promise<void> {
  console.info("== Checking all shows for airing episodes ==");
  const allShows = await getAllShows();

  for (const show of allShows) {
    try {
      await updateEpisodes(show.imdbId, show.tvdbId);
    } catch (error) {
      console.error(
        `Error updating episodes for ${show.name} (${show.imdbId})`,
        error,
      );
    }
  }

  console.info("== Finished checking all shows for airing episodes ==");
}

/**
 * Mark episodes as sent in the DB, just to avoid sending the same message twice
 * @param imdbId show to mark episodes as sent for
 * @param seasonNumber season to mark episodes as sent for
 * @param episodeNumber episode number(s) to mark as sent
 */
export async function markMessageSent(
  imdbId: string,
  seasonNumber: number,
  episodeNumber: number | number[],
): Promise<void> {
  const episodeNumbers: number[] = Array.isArray(episodeNumber)
    ? episodeNumber
    : [episodeNumber];

  const db = getDb();

  const showRow = await db
    .select({ id: shows.id })
    .from(shows)
    .where(eq(shows.imdbId, imdbId))
    .get();

  if (showRow == null) return;

  await db
    .update(episodes)
    .set({ messageSent: true })
    .where(
      and(
        eq(episodes.showId, showRow.id),
        eq(episodes.season, seasonNumber),
        inArray(episodes.number, episodeNumbers),
      ),
    );
}

/**
 * Creates a new episode notification subscription for a show
 * @param imdbId imdbID for the show to subscribe to
 * @param tvdbSeriesId tvdb id for the show
 * @param seriesName name of the tv show
 * @param destination where to send notifications
 * @returns the show after subscription is added
 */
export async function createNewSubscription(
  imdbId: string,
  tvdbSeriesId: number,
  seriesName: string,
  destination: Pick<Destination, "channelId" | "forumId">,
): Promise<Show> {
  const db = getDb();

  // Upsert show
  await db
    .insert(shows)
    .values({ imdbId, tvdbId: tvdbSeriesId, name: seriesName })
    .onConflictDoUpdate({
      target: shows.imdbId,
      set: { tvdbId: tvdbSeriesId, name: seriesName },
    });

  // Add destination
  const showRow = await db
    .select({ id: shows.id })
    .from(shows)
    .where(eq(shows.imdbId, imdbId))
    .get();

  if (showRow != null) {
    await db
      .insert(showDestinations)
      .values({
        showId: showRow.id,
        channelId: destination.channelId,
        forumId: destination.forumId,
      })
      .onConflictDoNothing();
  }

  const show = await getShowByImdbId(imdbId);
  if (show == null) {
    throw new Error(`Show ${imdbId} not found after creation`);
  }
  return show;
}

/**
 * Unsubscribe a channel from notifications for one or more shows
 * @param imdbIds imdb IDs for the shows to remove the subscription from
 * @param channelId channel to unsubscribe the shows from
 */
export async function removeSubscriptions(
  imdbIds: string[],
  channelId: string,
): Promise<void> {
  if (imdbIds.length === 0) return;
  const db = getDb();

  const showRows = await db
    .select({ id: shows.id })
    .from(shows)
    .where(inArray(shows.imdbId, imdbIds));

  if (showRows.length === 0) return;

  await db.delete(showDestinations).where(
    and(
      inArray(
        showDestinations.showId,
        showRows.map((r) => r.id),
      ),
      eq(showDestinations.channelId, channelId),
    ),
  );
}

/**
 * Unsubscribes a channel from all notifications
 * @param id id to use in the where clause
 * @param idType whether to use the channel id or forum id in the where clause
 */
export async function removeAllSubscriptions(
  id: string,
  idType: "channelId" | "forumId" = "channelId",
): Promise<void> {
  const db = getDb();
  const column =
    idType === "channelId"
      ? showDestinations.channelId
      : showDestinations.forumId;

  await db.delete(showDestinations).where(eq(column, id));
  console.info(`Deleted all show destinations for channel ${id}`);
}

/**
 * Removes all shows that have no destinations
 */
export async function pruneUnsubscribedShows(): Promise<void> {
  const db = getDb();

  const result = await db
    .delete(shows)
    .where(
      notInArray(
        shows.id,
        db
          .selectDistinct({ showId: showDestinations.showId })
          .from(showDestinations),
      ),
    );

  const count = result.meta.changes ?? 0;
  console.info(`Pruned shows ${count} with no destinations`);
}
