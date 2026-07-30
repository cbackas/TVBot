import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { MongoClient } from "mongodb";

const __dirname = fileURLToPath(new URL(".", import.meta.url));

const MONGODB_URI = process.env.MONGODB_URI;
if (!MONGODB_URI) {
	throw new Error("MONGODB_URI environment variable is required");
}

// Set MIGRATION_REMOTE=true to run against the production D1 database.
// Defaults to local for safety.
const REMOTE = process.env.MIGRATION_REMOTE === "true";

// The Mongo bot ran in a single Discord server, so every destination it holds
// belongs to that guild. Stamp it onto the guild-scoped rows in D1. Override
// with MIGRATION_GUILD_ID if migrating a different source server.
const GUILD_ID = process.env.MIGRATION_GUILD_ID ?? "1054158011742044160";

interface MongoEpisode {
	season: number;
	number: number;
	title?: string;
	airDate: Date | string;
	messageSent: boolean;
}

interface MongoDestination {
	channelId: string;
	forumId?: string;
}

interface MongoShow {
	name: string;
	imdbId: string;
	tvdbId: number;
	episodes: MongoEpisode[];
	destinations: MongoDestination[];
}

interface MongoSettings {
	_id: number;
	allEpisodes?: MongoDestination[];
	morningSummaryDestinations?: MongoDestination[];
	defaultForum?: string;
}

const sqlString = (value: string): string => `'${value.replace(/'/g, "''")}'`;
const sqlNullable = (value: string | undefined | null): string =>
	value == null ? "NULL" : sqlString(value);

const toIsoDate = (value: Date | string): string =>
	value instanceof Date ? value.toISOString() : new Date(value).toISOString();

async function main(): Promise<void> {
	const client = new MongoClient(MONGODB_URI as string);
	await client.connect();
	console.log("Connected to MongoDB");

	try {
		const db = client.db();

		const shows = (await db
			.collection("Show")
			.find()
			.toArray()) as unknown as MongoShow[];
		console.log(`Found ${shows.length} shows`);

		const settings = (await db
			.collection("Settings")
			.findOne({ _id: 0 as never })) as unknown as MongoSettings | null;
		console.log(`Settings found: ${!!settings}`);

		const lines: string[] = ["BEGIN TRANSACTION;"];
		const counts = {
			shows: 0,
			episodes: 0,
			showDestinations: 0,
			globalDestinations: 0,
			skippedShows: 0,
			skippedEpisodes: 0,
			skippedDestinations: 0,
		};

		const validShows: MongoShow[] = [];
		for (const show of shows) {
			if (
				!show.imdbId ||
				typeof show.tvdbId !== "number" ||
				!show.name
			) {
				console.warn(
					`Skipping malformed show (missing imdbId/tvdbId/name): ${JSON.stringify({
						name: show.name,
						imdbId: show.imdbId,
						tvdbId: show.tvdbId,
					})}`,
				);
				counts.skippedShows++;
				continue;
			}
			validShows.push(show);
			lines.push(
				`INSERT INTO shows (imdb_id, tvdb_id, name) VALUES (${sqlString(
					show.imdbId,
				)}, ${show.tvdbId}, ${sqlString(show.name)}) ON CONFLICT(imdb_id) DO NOTHING;`,
			);
			counts.shows++;
		}

		for (const show of validShows) {
			const showRef = `(SELECT id FROM shows WHERE imdb_id = ${sqlString(show.imdbId)})`;
			for (const ep of show.episodes ?? []) {
				if (
					ep.season == null ||
					ep.number == null ||
					ep.airDate == null
				) {
					counts.skippedEpisodes++;
					continue;
				}
				lines.push(
					`INSERT INTO episodes (show_id, season, number, title, air_date, message_sent) VALUES (${showRef}, ${ep.season}, ${ep.number}, ${sqlString(
						ep.title ?? "",
					)}, ${sqlString(toIsoDate(ep.airDate))}, ${ep.messageSent ? 1 : 0}) ON CONFLICT(show_id, season, number) DO NOTHING;`,
				);
				counts.episodes++;
			}
		}

		for (const show of validShows) {
			const showRef = `(SELECT id FROM shows WHERE imdb_id = ${sqlString(show.imdbId)})`;
			for (const dest of show.destinations ?? []) {
				if (!dest.channelId) {
					counts.skippedDestinations++;
					continue;
				}
				lines.push(
					`INSERT INTO show_destinations (show_id, guild_id, channel_id, forum_id) VALUES (${showRef}, ${sqlString(GUILD_ID)}, ${sqlString(dest.channelId)}, ${sqlNullable(dest.forumId)}) ON CONFLICT(show_id, channel_id) DO NOTHING;`,
				);
				counts.showDestinations++;
			}
		}

		// global_destinations type strings must match the runtime in
		// src/lib/settingsManager.ts:
		//   Mongo `allEpisodes`               -> 'global_episode_broadcast'
		//   Mongo `morningSummaryDestinations` -> 'morning_summary'
		//   Mongo `defaultForum` (string)      -> 'default_forum'
		if (settings) {
			for (const dest of settings.allEpisodes ?? []) {
				if (!dest.channelId) continue;
				lines.push(
					`INSERT INTO global_destinations (guild_id, channel_id, type) VALUES (${sqlString(GUILD_ID)}, ${sqlString(dest.channelId)}, 'global_episode_broadcast') ON CONFLICT(guild_id, channel_id, type) DO NOTHING;`,
				);
				counts.globalDestinations++;
			}
			for (const dest of settings.morningSummaryDestinations ?? []) {
				if (!dest.channelId) continue;
				if (dest.forumId) {
					// New schema only stores channel_id for morning_summary;
					// the old forumId hint is intentionally dropped.
					console.warn(
						`morning_summary destination channelId=${dest.channelId} had forumId=${dest.forumId} in Mongo; new schema does not preserve it`,
					);
				}
				lines.push(
					`INSERT INTO global_destinations (guild_id, channel_id, type) VALUES (${sqlString(GUILD_ID)}, ${sqlString(dest.channelId)}, 'morning_summary') ON CONFLICT(guild_id, channel_id, type) DO NOTHING;`,
				);
				counts.globalDestinations++;
			}
			if (settings.defaultForum) {
				lines.push(
					`INSERT INTO global_destinations (guild_id, channel_id, type) VALUES (${sqlString(GUILD_ID)}, ${sqlString(settings.defaultForum)}, 'default_forum') ON CONFLICT(guild_id, channel_id, type) DO NOTHING;`,
				);
				counts.globalDestinations++;
			}
		}

		lines.push("COMMIT;");

		const sqlFile = resolve(__dirname, "mongo-migration.sql");
		writeFileSync(sqlFile, lines.join("\n") + "\n");
		console.log(`Wrote ${lines.length} SQL statements to ${sqlFile}`);
		console.log(
			`  shows=${counts.shows} episodes=${counts.episodes} show_destinations=${counts.showDestinations} global_destinations=${counts.globalDestinations}`,
		);
		if (
			counts.skippedShows ||
			counts.skippedEpisodes ||
			counts.skippedDestinations
		) {
			console.warn(
				`  skipped: shows=${counts.skippedShows} episodes=${counts.skippedEpisodes} destinations=${counts.skippedDestinations}`,
			);
		}

		console.log(
			`Executing SQL against ${REMOTE ? "REMOTE (production)" : "local"} D1 database...`,
		);
		execFileSync(
			"npx",
			[
				"wrangler",
				"d1",
				"execute",
				"DB",
				REMOTE ? "--remote" : "--local",
				`--file=${sqlFile}`,
			],
			{
				stdio: "inherit",
				cwd: resolve(__dirname, ".."),
			},
		);
		console.log("Migration complete!");
	} finally {
		await client.close();
	}
}

main().catch((err) => {
	console.error("Migration failed:", err);
	process.exit(1);
});
