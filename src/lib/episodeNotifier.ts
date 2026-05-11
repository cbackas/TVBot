import moment from "moment-timezone";
import { getDb } from "../database/db.js";
import { showSchema } from "../database/types.js";
import { assert } from "../utils.js";
import { getEnv } from "./env.js";
import { getGlobalDestinations } from "./settingsManager.js";
import type { Show } from "./shows.js";
import { markMessageSent } from "./shows.js";
import { addLeadingZeros, toRanges } from "./util.js";

export interface NotificationPayload {
  key: string;
  timestamp: number;
  imdbId: string;
  showName: string;
  season: number;
  episodeNumbers: number[];
  destinations: Show["destinations"];
}

/**
 * Send messages for all the shows that have episodes airing in the next few minutes
 * @returns a promise that resolves when all the messages have been sent
 */
export async function sendAiringMessages(): Promise<void> {
  const token = getEnv("DISCORD_TOKEN");
  const globalDestinations = await getGlobalDestinations("all_episodes");

  const payloadMap = await getShowPayloads();
  for (const payload of payloadMap.values()) {
    await sendNotificationPayload(payload, token, globalDestinations);
  }
}

/**
 * Get all the shows that have episodes airing in the next x minutes
 * @param minutes how many minutes in the future to look for shows
 * @returns a map of payloads for each show that has an episode airing in the next x minutes
 */
async function getShowPayloads(
  minutes: number = 5,
): Promise<Map<string, NotificationPayload>> {
  const nowUtc = moment.utc();
  const minutesFromNow = nowUtc.clone().add(minutes, "minutes");

  const db = getDb();
  const rows = await db.query.shows.findMany({
    with: { episodes: true, destinations: true },
  });
  const allShows: Show[] = rows.map((r) => showSchema.parse(r));

  // Filter to only shows that have unsent episodes within the time window
  const showsWithEpisodes = allShows.filter((show) =>
    show.episodes.some(
      (e) =>
        !e.messageSent && moment.utc(e.airDate).isSameOrBefore(minutesFromNow),
    ),
  );

  // Convert the shows into a map of notification payloads
  return showsWithEpisodes.reduce(
    (acc: Map<string, NotificationPayload>, show: Show) => {
      const momentUTC = moment.utc(new Date());

      for (const e of show.episodes) {
        const airDate = moment.utc(e.airDate);
        const inTimeWindow =
          airDate.isSameOrAfter(momentUTC) &&
          airDate.isSameOrBefore(momentUTC.clone().add(minutes, "minutes"));

        if (!inTimeWindow) continue;

        const key = `announceEpisodes:${show.imdbId}:S${addLeadingZeros(
          e.season,
          2,
        )}`;

        // define the default payload to use if one doesn't exist in the map
        const defaultPayload: NotificationPayload = {
          key,
          timestamp: airDate.unix(),
          imdbId: show.imdbId,
          showName: show.name,
          season: e.season,
          episodeNumbers: [], // it has an empty array of episode numbers because it will be filled in later
          destinations: show.destinations,
        };

        // grab the payload from the map or create a new one
        if (!acc.has(key)) acc.set(key, defaultPayload);
        const payload = acc.get(key);
        assert(payload != null);

        // add the episode number to the payload
        payload.episodeNumbers.push(e.number);
      }

      return acc;
    },
    new Map<string, NotificationPayload>(),
  );
}

/**
 * Send a message to a Discord channel via REST API
 * @param channelId the channel to send the message to
 * @param content the message content
 * @param token the bot token for authorization
 */
async function sendDiscordMessage(
  channelId: string,
  content: string,
  token: string,
): Promise<void> {
  const response = await fetch(
    `https://discord.com/api/v10/channels/${channelId}/messages`,
    {
      method: "POST",
      headers: {
        Authorization: `Bot ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content }),
    },
  );
  if (!response.ok) {
    throw new Error(
      `Discord API error ${response.status}: ${await response.text()}`,
    );
  }
}

/**
 * Send discord messages to episode and global destinations with info about the episode(s)
 * @param payload all the info needed to schedule a notification job
 * @param token bot token needed to send the messages
 * @param globalDestinations additional destinations to send the message to
 */
async function sendNotificationPayload(
  payload: NotificationPayload,
  token: string,
  globalDestinations: { channelId: string }[],
): Promise<void> {
  const message = getEpisodeMessage(
    payload.showName,
    payload.season,
    payload.episodeNumbers,
    payload.timestamp,
  );

  // send the message to all the channels subscribed to the show
  for (const destination of payload.destinations) {
    try {
      await sendDiscordMessage(destination.channelId, message, token);
      console.info(`Message Sent: ${message}`);
    } catch (e) {
      console.error("Error sending message to destination", e);
    }
  }

  // build the message that's sent to the global destinations
  const channelsString = payload.destinations
    .map((d) => `<#${d.channelId}>`)
    .join(" ");
  const globalMessage = `${message} Check out the discussions here: ${channelsString}`;

  // send messages to all the global destinations
  for (const destination of globalDestinations) {
    try {
      await sendDiscordMessage(destination.channelId, globalMessage, token);
      console.info(`Message Sent: ${globalMessage}`);
    } catch (e) {
      console.error("Error sending message to global destination", e);
    }
  }

  // mark message as sent in the db
  await markMessageSent(payload.imdbId, payload.season, payload.episodeNumbers);
}

/**
 * builds the message thats sent to discord for an airing episode
 * @param showName name of the show for the message
 * @param season name of the season for the message
 * @param episodeNumbers episodes being announced in the message
 * @returns message to send to discord
 * @throws if there are no episodes to schedule
 */
function getEpisodeMessage(
  showName: string,
  season: number,
  episodeNumbers: number[],
  timestamp: number,
): string {
  if (episodeNumbers.length <= 0) {
    throw new Error("No episodes to schedule");
  }

  if (episodeNumbers.length === 1) {
    return `**${showName} S${addLeadingZeros(season, 2)}E${addLeadingZeros(
      episodeNumbers[0],
      2,
    )}** is airing <t:${timestamp}:R>`;
  }

  return `**${showName} S${addLeadingZeros(season, 2)}E${toRanges(
    episodeNumbers,
  ).join(",")}** is streaming somewhere <t:${timestamp}:R>!`;
}
