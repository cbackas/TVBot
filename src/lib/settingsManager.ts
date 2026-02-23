import { and, eq } from "drizzle-orm";
import { getDb } from "../database/db.js";
import { globalDestinations } from "../database/schema.js";
import type { GlobalDestination } from "../database/types.js";

export type { GlobalDestination };
export type GlobalDestinationType =
  | "all_episodes"
  | "morning_summary"
  | "tv_forum";

export async function getGlobalDestinations(
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
): Promise<GlobalDestination[]> {
  const db = getDb();
  await db
    .insert(globalDestinations)
    .values({ channelId, type })
    .onConflictDoNothing();
  return getGlobalDestinations(type);
}

export async function removeGlobalDestination(
  type: GlobalDestinationType,
  channelId: string,
): Promise<GlobalDestination[]> {
  const db = getDb();
  await db
    .delete(globalDestinations)
    .where(
      and(
        eq(globalDestinations.channelId, channelId),
        eq(globalDestinations.type, type),
      ),
    );
  return getGlobalDestinations(type);
}

export async function getDefaultForum(): Promise<string | null> {
  const rows = await getGlobalDestinations("tv_forum");
  return rows[0]?.channelId ?? null;
}

export async function setDefaultForum(channelId: string): Promise<void> {
  const db = getDb();
  const existing = await getGlobalDestinations("tv_forum");
  if (existing.length > 0) {
    await db
      .update(globalDestinations)
      .set({ channelId })
      .where(eq(globalDestinations.type, "tv_forum"));
  } else {
    await db.insert(globalDestinations).values({ channelId, type: "tv_forum" });
  }
}
