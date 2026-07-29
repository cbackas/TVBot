import { type InferInsertModel, inArray } from "drizzle-orm";
import type { BatchItem } from "drizzle-orm/batch";
import type {
  SQLiteColumn,
  SQLiteInsert,
  SQLiteInsertOnConflictDoUpdateConfig,
  SQLiteTable,
} from "drizzle-orm/sqlite-core";
import type { getDb } from "./db.js";

type Db = ReturnType<typeof getDb>;

/**
 * D1 rejects any single statement that binds more than this many parameters
 * with `too many SQL variables`.
 * https://developers.cloudflare.com/d1/platform/limits/
 */
export const D1_MAX_BOUND_PARAMS = 100;

/** Stay comfortably under the hard cap rather than sitting on the boundary. */
const SAFE_PARAM_BUDGET = 90;

/**
 * Yield slices of `items` sized so each fits within D1's bound-parameter cap.
 *
 * Params-per-item is auto-derived from the first item's column count, so bulk
 * inserts/upserts just pass their rows. For scalar lists (e.g. an `inArray(...)`
 * of ids), each item is one bound param — pass `paramsPerItem: 1`.
 *
 * Adapted from the community generator in
 * https://github.com/drizzle-team/drizzle-orm/issues/1740 — the constant is
 * D1's 100-param cap rather than SQLite/Postgres' larger default.
 */
export function* chunkForD1<T>(
  items: readonly T[],
  paramsPerItem = items[0] ? Object.keys(items[0]).length : 1,
): Generator<T[]> {
  const size = Math.max(1, Math.floor(SAFE_PARAM_BUDGET / paramsPerItem));
  for (let i = 0; i < items.length; i += size) {
    yield items.slice(i, i + size);
  }
}

/**
 * Run pre-chunked statements as a single atomic batch. No-op when empty, so
 * callers don't have to guard against sending nothing.
 */
export async function runBatch(
  db: Db,
  statements: BatchItem<"sqlite">[],
): Promise<void> {
  // Destructuring narrows to a non-empty tuple, which is what db.batch's type
  // wants — so no cast, and the empty case is handled by the same check.
  const [first, ...rest] = statements;
  if (first === undefined) return;
  await db.batch([first, ...rest]);
}

// ── Bulk-write wrappers ──────────────────────────────────────────────────────
// Pass the whole array; these derive the per-row parameter count, split under
// D1's cap, and run the chunks as one atomic batch. Chunking stays invisible.

/** Bulk insert `rows`, chunked to stay under D1's parameter cap. */
export async function chunkedInsert<TTable extends SQLiteTable>(
  db: Db,
  table: TTable,
  rows: InferInsertModel<TTable>[],
): Promise<void> {
  const statements = [...chunkForD1(rows)].map((chunk) =>
    db.insert(table).values(chunk),
  );
  await runBatch(db, statements);
}

/** Bulk upsert `rows` with an ON CONFLICT DO UPDATE clause, chunked. */
export async function chunkedUpsert<TTable extends SQLiteTable>(
  db: Db,
  table: TTable,
  rows: InferInsertModel<TTable>[],
  conflict: SQLiteInsertOnConflictDoUpdateConfig<SQLiteInsert<TTable>>,
): Promise<void> {
  const statements = [...chunkForD1(rows)].map((chunk) =>
    db.insert(table).values(chunk).onConflictDoUpdate(conflict),
  );
  await runBatch(db, statements);
}

/** Delete every row whose `column` is in `ids`, chunked (1 param per id). */
export async function chunkedDelete<TColumn extends SQLiteColumn>(
  db: Db,
  table: SQLiteTable,
  column: TColumn,
  ids: TColumn["_"]["data"][],
): Promise<void> {
  const statements = [...chunkForD1(ids, 1)].map((chunk) =>
    db.delete(table).where(inArray(column, chunk)),
  );
  await runBatch(db, statements);
}
