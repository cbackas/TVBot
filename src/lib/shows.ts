import { and, asc, eq, inArray, like, notInArray, or, sql } from "drizzle-orm";
import moment, { type Moment } from "moment-timezone";
import { getDb } from "../database/db.js";
import {
  episodes,
  globalDestinations,
  showDestinations,
  shows,
} from "../database/schema.js";
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
): Promise<{ episodesFound: number; newEpisodes: number }> {
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

  // Count episodes we haven't seen before (present in TVDB, absent from the DB)
  let newEpisodes = 0;

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

    // Upsert by (showId, season, number) so IDs and messageSent persist across refreshes.
    // Then delete every existing episode that isn't in TVDB's current upcoming list —
    // this catches both TVDB-cancelled future episodes and episodes that have aired since
    // the last refresh. messageSent history is intentionally not retained.
    const existing = await db
      .select({
        id: episodes.id,
        season: episodes.season,
        number: episodes.number,
      })
      .from(episodes)
      .where(eq(episodes.showId, showRow.id));

    const existingKeys = new Set(
      existing.map((e) => `${e.season}-${e.number}`),
    );
    newEpisodes = insertValues.filter(
      (v) => !existingKeys.has(`${v.season}-${v.number}`),
    ).length;

    const incomingKeys = new Set(
      insertValues.map((v) => `${v.season}-${v.number}`),
    );
    const idsToDelete = existing
      .filter((e) => !incomingKeys.has(`${e.season}-${e.number}`))
      .map((e) => e.id);

    // Apply the upsert + deletes in one atomic batch. Each row is its own
    // statement (≈6 bound params), so no single statement approaches D1's
    // 100-param cap — no chunking math required.
    const ops = [
      ...insertValues.map((v) =>
        db
          .insert(episodes)
          .values(v)
          .onConflictDoUpdate({
            target: [episodes.showId, episodes.season, episodes.number],
            set: {
              title: sql`excluded.title`,
              airDate: sql`excluded.air_date`,
            },
          }),
      ),
      ...idsToDelete.map((id) =>
        db.delete(episodes).where(eq(episodes.id, id)),
      ),
    ];

    const [first, ...rest] = ops;
    if (first !== undefined) await db.batch([first, ...rest]);
  }

  console.info(
    `[Get Episode Data] ${series.name} / Upcoming episodes: ${upcomingEpisodes.length}`,
  );

  return { episodesFound: upcomingEpisodes.length, newEpisodes };
}

/**
 * Updates all shows in the DB with new episodes
 */
export interface RefreshStats {
  showsTotal: number;
  showsRefreshed: number;
  showsFailed: number;
  episodesFound: number;
  newEpisodes: number;
}

export async function checkForAiringEpisodes(): Promise<RefreshStats> {
  console.info("== Checking all shows for airing episodes ==");
  const allShows = await getAllShows();

  const stats: RefreshStats = {
    showsTotal: allShows.length,
    showsRefreshed: 0,
    showsFailed: 0,
    episodesFound: 0,
    newEpisodes: 0,
  };

  for (const show of allShows) {
    try {
      const { episodesFound, newEpisodes } = await updateEpisodes(
        show.imdbId,
        show.tvdbId,
      );
      stats.showsRefreshed += 1;
      stats.episodesFound += episodesFound;
      stats.newEpisodes += newEpisodes;
    } catch (error) {
      stats.showsFailed += 1;
      console.error(
        `Error updating episodes for ${show.name} (${show.imdbId})`,
        error,
      );
    }
  }

  console.info("== Finished checking all shows for airing episodes ==");
  return stats;
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
export async function pruneUnsubscribedShows(): Promise<number> {
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
  return count;
}

/**
 * Delete every destination row pointing at a channel we know is gone.
 * Idempotent — safe to call on every Discord 10003 we see.
 */
export async function pruneDeadChannel(channelId: string): Promise<void> {
  const db = getDb();
  await db.batch([
    db
      .delete(showDestinations)
      .where(eq(showDestinations.channelId, channelId)),
    db
      .delete(globalDestinations)
      .where(eq(globalDestinations.channelId, channelId)),
  ]);
  console.info(`Pruned dead channel ${channelId}`);
}

/**
 * Probe every distinct destination channelId against Discord; prune the dead ones.
 * Catches channels deleted while the bot had no upcoming work to surface them.
 */
export async function sweepDeadChannels(
  token: string,
): Promise<{ probed: number; pruned: number }> {
  const db = getDb();
  const [showRows, globalRows] = await db.batch([
    db
      .selectDistinct({ channelId: showDestinations.channelId })
      .from(showDestinations),
    db
      .selectDistinct({ channelId: globalDestinations.channelId })
      .from(globalDestinations),
  ]);
  const distinct = new Set<string>([
    ...showRows.map((r) => r.channelId),
    ...globalRows.map((r) => r.channelId),
  ]);

  let pruned = 0;
  for (const channelId of distinct) {
    try {
      const response = await fetch(
        `https://discord.com/api/v10/channels/${channelId}`,
        { headers: { Authorization: `Bot ${token}` } },
      );
      if (response.ok) continue;
      if (response.status !== 404) continue;
      const body = (await response.json().catch(() => ({}))) as {
        code?: number;
      };
      if (body.code === 10003) {
        await pruneDeadChannel(channelId);
        pruned += 1;
      }
    } catch (e) {
      console.error(`Sweep: error probing channel ${channelId}`, e);
    }
  }

  return { probed: distinct.size, pruned };
}
