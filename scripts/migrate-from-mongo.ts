import { MongoClient } from "mongodb";
import { writeFileSync } from "node:fs";
import { execSync } from "node:child_process";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = fileURLToPath(new URL(".", import.meta.url));

const MONGODB_URI = process.env.MONGODB_URI;
if (!MONGODB_URI) {
	throw new Error("MONGODB_URI environment variable is required");
}

function escapeSQL(value: string): string {
	return value.replace(/'/g, "''");
}

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
	allEpisodes: MongoDestination[];
	morningSummaryDestinations: MongoDestination[];
	defaultForum?: string;
}

async function main() {
	const client = new MongoClient(MONGODB_URI!);
	await client.connect();
	console.log("Connected to MongoDB");

	const db = client.db();
	// const shows: MongoShow[] = await db.collection("Show").find().toArray() as unknown as MongoShow[];
	// console.log(`Found ${shows.length} shows`);
	//
	// const settings = await db.collection("Settings").findOne({ _id: 0 as any }) as unknown as MongoSettings | null;
	// console.log(`Settings found: ${!!settings}`);
	//
	// await client.close();
	//
	// const lines: string[] = [];
	//
	// // Insert shows
	// for (const show of shows) {
	// 	lines.push(
	// 		`INSERT INTO shows (imdb_id, tvdb_id, name) VALUES ('${escapeSQL(show.imdbId)}', ${show.tvdbId}, '${escapeSQL(show.name)}');`
	// 	);
	// }
	//
	// // Insert episodes (FK lookup via imdb_id)
	// for (const show of shows) {
	// 	for (const ep of show.episodes) {
	// 		const airDate = ep.airDate instanceof Date
	// 			? ep.airDate.toISOString()
	// 			: new Date(ep.airDate).toISOString();
	// 		const messageSent = ep.messageSent ? 1 : 0;
	// 		const title = escapeSQL(ep.title ?? "");
	//
	// 		lines.push(
	// 			`INSERT INTO episodes (show_id, season, number, title, air_date, message_sent) VALUES ((SELECT id FROM shows WHERE imdb_id = '${escapeSQL(show.imdbId)}'), ${ep.season}, ${ep.number}, '${title}', '${airDate}', ${messageSent});`
	// 		);
	// 	}
	// }
	//
	// // Insert show_destinations (FK lookup via imdb_id)
	// for (const show of shows) {
	// 	for (const dest of show.destinations) {
	// 		const forumId = dest.forumId ? `'${escapeSQL(dest.forumId)}'` : "NULL";
	// 		lines.push(
	// 			`INSERT INTO show_destinations (show_id, channel_id, forum_id) VALUES ((SELECT id FROM shows WHERE imdb_id = '${escapeSQL(show.imdbId)}'), '${escapeSQL(dest.channelId)}', ${forumId});`
	// 		);
	// 	}
	// }
	//
	// // Insert global_destinations from Settings
	// if (settings) {
	// 	for (const dest of settings.allEpisodes ?? []) {
	// 		lines.push(
	// 			`INSERT INTO global_destinations (channel_id, type) VALUES ('${escapeSQL(dest.channelId)}', 'all_episodes');`
	// 		);
	// 	}
	// 	for (const dest of settings.morningSummaryDestinations ?? []) {
	// 		lines.push(
	// 			`INSERT INTO global_destinations (channel_id, type) VALUES ('${escapeSQL(dest.channelId)}', 'morning_summary');`
	// 		);
	// 	}
	// 	if (settings.defaultForum) {
	// 		lines.push(
	// 			`INSERT INTO global_destinations (channel_id, type) VALUES ('${escapeSQL(settings.defaultForum)}', 'tv_forum');`
	// 		);
	// 	}
	// }
	//
	const sqlFile = resolve(__dirname, "mongo-migration.sql");
	// writeFileSync(sqlFile, lines.join("\n") + "\n");
	// console.log(`Wrote ${lines.length} SQL statements to ${sqlFile}`);

	console.log("Executing SQL against local D1...");
	execSync(`npx wrangler d1 execute DB --remote --file=${sqlFile}`, {
		stdio: "inherit",
		cwd: resolve(__dirname, ".."),
	});
	console.log("Migration complete!");
}

main().catch((err) => {
	console.error("Migration failed:", err);
	process.exit(1);
});
