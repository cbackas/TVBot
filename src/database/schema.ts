import {
  foreignKey,
  index,
  integer,
  sqliteTable,
  text,
  unique,
} from "drizzle-orm/sqlite-core";

export const shows = sqliteTable("shows", {
  id: integer().primaryKey({ autoIncrement: true }),
  imdbId: text("imdb_id").notNull().unique(),
  tvdbId: integer("tvdb_id").notNull(),
  name: text().notNull(),
});

export const episodes = sqliteTable(
  "episodes",
  {
    id: integer().primaryKey({ autoIncrement: true }),
    showId: integer("show_id").notNull(),
    season: integer().notNull(),
    number: integer().notNull(),
    title: text().notNull().default(""),
    airDate: text("air_date").notNull(),
    messageSent: integer("message_sent", { mode: "boolean" })
      .notNull()
      .default(false),
  },
  (table) => [
    foreignKey({
      columns: [table.showId],
      foreignColumns: [shows.id],
    }).onDelete("cascade"),
    unique().on(table.showId, table.season, table.number),
    index("idx_episodes_show_id").on(table.showId),
    index("idx_episodes_air_date").on(table.airDate),
    index("idx_episodes_message_sent").on(table.messageSent),
  ],
);

export const globalDestinations = sqliteTable(
  "global_destinations",
  {
    id: integer().primaryKey({ autoIncrement: true }),
    channelId: text("channel_id").notNull(),
    type: text().notNull(), // "all_episodes" | "morning_summary"
  },
  (table) => [
    unique().on(table.channelId, table.type),
    index("idx_global_destinations_type").on(table.type),
  ],
);

export const showDestinations = sqliteTable(
  "show_destinations",
  {
    id: integer().primaryKey({ autoIncrement: true }),
    showId: integer("show_id").notNull(),
    channelId: text("channel_id").notNull(),
    forumId: text("forum_id"),
  },
  (table) => [
    foreignKey({
      columns: [table.showId],
      foreignColumns: [shows.id],
    }).onDelete("cascade"),
    unique().on(table.showId, table.channelId),
    index("idx_show_destinations_show_id").on(table.showId),
    index("idx_show_destinations_channel_id").on(table.channelId),
  ],
);
