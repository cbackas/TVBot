import type { APIEmbed, APIEmbedField } from "discord-api-types/v10";
import moment from "moment-timezone";
import { assert } from "../utils.js";
import { getEnv } from "./env.js";
import type { NotificationPayload } from "./episodeNotifier.js";
import type { Show } from "./shows.js";
import { addLeadingZeros, toRanges } from "./util.js";

interface UpcomingEpisodeMessages {
  prefix: string;
  empty: string;
  messages: string[];
  embedFields: APIEmbedField[];
}

function getShowMessages(
  shows: Show[],
  days: number = 1,
): UpcomingEpisodeMessages {
  if (days <= 0) throw new Error("days must be greater than 0");

  const payloadCollection = reduceEpisodes(shows, days);

  const sortedPayloads = [...payloadCollection.values()].sort((p1, p2) => {
    return p1.timestamp - p2.timestamp;
  });

  const messages = sortedPayloads.map((payload) => {
    const seasonNumber = addLeadingZeros(payload.season, 2);
    const episodeNumbers = toRanges(payload.episodeNumbers);
    const message = `**${payload.showName}** S${seasonNumber}E${episodeNumbers.join(
      ",",
    )} - <t:${payload.timestamp}:R>`;
    return message;
  });

  const embedFieldsMap = sortedPayloads.reduce((acc, payload) => {
    const seasonNumber = addLeadingZeros(payload.season, 2);
    const episodeNumbers = toRanges(payload.episodeNumbers);
    const message = `**${payload.showName}** S${seasonNumber}E${episodeNumbers.join(
      ",",
    )} - <t:${payload.timestamp}:R>`;
    const airDate = moment.unix(payload.timestamp).tz(getEnv("TZ"));
    const key = airDate.format("dddd - Do of MMMM");
    if (!acc.has(key)) acc.set(key, []);
    acc.get(key)?.push(message);
    return acc;
  }, new Map<string, string[]>());

  const embedFields = [...embedFieldsMap.entries()].map(
    ([airDate, messages]) => {
      return {
        name: airDate,
        value: messages.join("\n"),
      };
    },
  );

  let prefixString: string = "Shows airing ";
  let emptyString: string = "No shows airing ";

  if (days === 1) {
    prefixString += "in the next day";
    emptyString += "today";
  } else if (days === 7) {
    prefixString += "this week";
    emptyString += "this week";
  } else {
    prefixString += `in the next ${days} days`;
    emptyString += `in the next ${days} days`;
  }

  return {
    prefix: prefixString,
    empty: emptyString,
    embedFields,
    messages,
  };
}

/**
 * Gets upcoming episodes for the next X days and returns a string message to send to users
 * @param days number of days to look ahead
 * @returns a string message to send to Discord
 */
export function getUpcomingEpisodesMessage(
  shows: Show[],
  days: number = 1,
): string {
  const messages = getShowMessages(shows, days);

  return messages.messages.length >= 1
    ? `${messages.prefix}:\n\n${messages.messages.join("\n")}`
    : messages.empty;
}

export function getUpcomingEpisodesEmbed(
  shows: Show[],
  days: number = 1,
): APIEmbed {
  const messages = getShowMessages(shows, days);

  const footer: APIEmbed["footer"] = {
    text: "Powered by TVDB",
    icon_url: "https://www.thetvdb.com/images/logo.png",
  };

  if (messages.messages.length === 0) {
    return {
      title: messages.empty,
      footer,
    };
  }

  return {
    title: messages.prefix,
    fields: messages.embedFields,
    footer,
  };
}

/**
 * reduce function that converts a list of shows into a collection of notification payloads
 * @param shows list of shows to process
 * @param days number of days to look ahead
 * @returns collection of notification payloads
 */
function reduceEpisodes(
  shows: Show[],
  days: number = 1,
): Map<string, NotificationPayload> {
  return shows.reduce((acc: Map<string, NotificationPayload>, show: Show) => {
    const momentUTC = moment.utc(new Date());

    for (const e of show.episodes) {
      const airDate = moment.utc(e.airDate);
      const inTimeWindow =
        airDate.isSameOrAfter(momentUTC) &&
        airDate.isSameOrBefore(momentUTC.clone().add(days, "day"));

      if (!inTimeWindow) continue;

      const airDateString = moment
        .utc(e.airDate)
        .tz(getEnv("TZ"))
        .format("YYYY-MM-DD@HH:mm");

      const key = `announceEpisodes:${airDateString}:${show.imdbId}:S${addLeadingZeros(
        e.season,
        2,
      )}`;

      // define the default payload to use if one doesn't exist in the collection
      const defaultPayload: NotificationPayload = {
        key,
        timestamp: airDate.unix(),
        imdbId: show.imdbId,
        showName: show.name,
        season: e.season,
        episodeNumbers: [], // it has an emtpy array of episode numbers because it will be filled in later
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
  }, new Map<string, NotificationPayload>());
}
