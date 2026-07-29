import { and, eq } from "drizzle-orm";
import { getDb } from "../database/db.js";
import { globalDestinations } from "../database/schema.js";
import type { GlobalDestination } from "../database/types.js";

export type { GlobalDestination };
export type GlobalDestinationType =
  | "default_forum"
  | "global_episode_broadcast"
  | "morning_summary";

export async function getGlobalDestinations(
  type: GlobalDestinationType,
  guildId: string,
): Promise<GlobalDestination[]> {
  const db = getDb();
  return db
    .select()
    .from(globalDestinations)
    .where(
      and(
        eq(globalDestinations.type, type),
        eq(globalDestinations.guildId, guildId),
      ),
    );
}

/**
 * Every destination of a type across all guilds. Used by scheduled jobs that
 * fan out per guild (broadcast, morning digest) rather than serving one guild's
 * interaction.
 */
export async function getAllGlobalDestinations(
  type: GlobalDestinationType,
): Promise<GlobalDestination[]> {
  const db = getDb();
  return db
    .select()
    .from(globalDestinations)
    .where(eq(globalDestinations.type, type));
}

export async function addGlobalDestination(
  type: GlobalDestinationType,
  channelId: string,
  guildId: string,
): Promise<GlobalDestination[]> {
  const db = getDb();
  await db
    .insert(globalDestinations)
    .values({ channelId, type, guildId })
    .onConflictDoNothing();
  return getGlobalDestinations(type, guildId);
}

export async function removeGlobalDestination(
  type: GlobalDestinationType,
  channelId: string,
  guildId: string,
): Promise<GlobalDestination[]> {
  const db = getDb();
  await db
    .delete(globalDestinations)
    .where(
      and(
        eq(globalDestinations.channelId, channelId),
        eq(globalDestinations.type, type),
        eq(globalDestinations.guildId, guildId),
      ),
    );
  return getGlobalDestinations(type, guildId);
}

export async function getDefaultForum(guildId: string): Promise<string | null> {
  const rows = await getGlobalDestinations("default_forum", guildId);
  return rows[0]?.channelId ?? null;
}

export async function setDefaultForum(
  channelId: string,
  guildId: string,
): Promise<void> {
  const db = getDb();
  const existing = await getGlobalDestinations("default_forum", guildId);
  if (existing.length > 0) {
    await db
      .update(globalDestinations)
      .set({ channelId })
      .where(
        and(
          eq(globalDestinations.type, "default_forum"),
          eq(globalDestinations.guildId, guildId),
        ),
      );
  } else {
    await db
      .insert(globalDestinations)
      .values({ channelId, type: "default_forum", guildId });
  }
}
