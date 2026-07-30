import type { APIEmbed, APIEmbedField } from "discord-api-types/v10";
import type { Destination } from "../database/types.js";
import type { SeriesExtendedRecord } from "../interfaces/tvdb.generated.js";
import { handleChannelSendError } from "./discord.js";

export async function sendDiscordEmbed(
  channelId: string,
  embed: APIEmbed,
  token: string,
  content: string = "",
): Promise<void> {
  const response = await fetch(
    `https://discord.com/api/v10/channels/${channelId}/messages`,
    {
      method: "POST",
      headers: {
        Authorization: `Bot ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content, embeds: [embed] }),
    },
  );
  if (!response.ok) {
    await handleChannelSendError(response, channelId, "Send embed to channel");
  }
}

export function buildShowEmbed(
  imdbId: string,
  tvdbSeries: SeriesExtendedRecord,
  destinations: Destination[] = [],
): APIEmbed {
  const country = tvdbSeries.latestNetwork?.country ?? "usa";

  // put together some basic data fields
  const fields: APIEmbedField[] = [
    {
      name: "Network",
      value: `${
        tvdbSeries.latestNetwork?.name ?? "unknown"
      } (${country.toUpperCase()})`,
      inline: true,
    },
    {
      name: "Status",
      value: tvdbSeries.status.name,
      inline: true,
    },
    {
      name: "Seasons",
      value: tvdbSeries.seasons.length.toFixed(0),
      inline: true,
    },
    {
      name: "Genres",
      value: tvdbSeries.genres.map((g) => g.name).join(", "),
      inline: true,
    },
  ];

  // add the linked channels field if there are any
  if (destinations.length > 0) {
    fields.push({
      name: "Linked Channels",
      value: destinations.map((d) => `<#${d.channelId}>`).join("\n"),
      inline: true,
    });
  }

  // add the links field
  fields.push({
    name: "Links",
    value: getLinks(tvdbSeries.remoteIds).join("\n"),
    inline: true,
  });

  // build and return the final embed object
  return {
    title: tvdbSeries.name,
    url: `https://www.imdb.com/title/${imdbId}`,
    thumbnail: {
      url: tvdbSeries.image,
    },
    description: tvdbSeries.overview,
    fields,
    footer: {
      text: "Powered by TVDB",
      icon_url: "https://www.thetvdb.com/images/logo.png",
    },
  };
}

export async function createForumThread(
  forumChannelId: string,
  name: string,
  embed: APIEmbed,
  token: string,
): Promise<{ threadId: string; messageId: string }> {
  const response = await fetch(
    `https://discord.com/api/v10/channels/${forumChannelId}/threads`,
    {
      method: "POST",
      headers: {
        Authorization: `Bot ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name,
        auto_archive_duration: 10080,
        message: { embeds: [embed] },
      }),
    },
  );
  if (!response.ok) {
    throw new Error(
      `Create forum thread "${name}" in ${forumChannelId} — Discord API error ${response.status}: ${await response.text()}`,
    );
  }
  const data: { id: string; last_message_id?: string } = await response.json();
  return { threadId: data.id, messageId: data.last_message_id ?? data.id };
}

export async function pinMessage(
  channelId: string,
  messageId: string,
  token: string,
): Promise<void> {
  const response = await fetch(
    `https://discord.com/api/v10/channels/${channelId}/pins/${messageId}`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bot ${token}`,
      },
    },
  );
  if (!response.ok) {
    throw new Error(
      `Pin message ${messageId} in channel ${channelId} — Discord API error ${response.status}: ${await response.text()}`,
    );
  }
}

export async function deleteChannel(
  channelId: string,
  token: string,
): Promise<void> {
  const response = await fetch(
    `https://discord.com/api/v10/channels/${channelId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bot ${token}`,
      },
    },
  );
  if (!response.ok) {
    throw new Error(
      `Delete channel ${channelId} — Discord API error ${response.status}: ${await response.text()}`,
    );
  }
}

function getLinks(remoteIds: SeriesExtendedRecord["remoteIds"]): string[] {
  const links: string[] = [];

  for (const remote of remoteIds) {
    if (remote.id == null) continue;

    if (remote.type === 2) {
      links.push(`[IMDB](https://www.imdb.com/title/${remote.id})`);
    }
    if (remote.type === 4) links.push(`[Official Site](${remote.id})`);
    if (remote.type === 24) {
      links.push(`[Wikipedia](https://en.wikipedia.org/wiki/${remote.id})`);
    }
    if (remote.type === 19) {
      links.push(`[TV Maze](https://www.tvmaze.com/shows/${remote.id})`);
    }
    if (remote.type === 9) {
      links.push(`[Instagram](https://www.instagram.com/${remote.id})`);
    }
    if (remote.type === 6) {
      links.push(`[Twitter](https://twitter.com/${remote.id})`);
    }
  }

  return links;
}
