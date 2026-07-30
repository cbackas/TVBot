import { getEnv } from "./env.js";
import { sendDiscordEmbed } from "./messages.js";
import { getAllGlobalDestinations } from "./settingsManager.js";
import { getAllShows } from "./shows.js";
import { getUpcomingEpisodesEmbed } from "./upcoming.js";

export async function sendMorningSummary(): Promise<void> {
  const destinations = await getAllGlobalDestinations("morning_summary");
  if (destinations.length === 0) {
    console.info(
      "Morning summary: no `morning_summary` destinations configured — nothing to send. Use `/setting morning_summary add`.",
    );
    return;
  }

  // Fan out per guild: each guild's digest only covers the shows linked within
  // that guild, so servers don't see each other's shows.
  const channelsByGuild = new Map<string, string[]>();
  for (const dest of destinations) {
    const list = channelsByGuild.get(dest.guildId) ?? [];
    list.push(dest.channelId);
    channelsByGuild.set(dest.guildId, list);
  }

  const allShows = await getAllShows();
  const token = getEnv("DISCORD_TOKEN");

  let sent = 0;
  let total = 0;
  for (const [guildId, channelIds] of channelsByGuild) {
    const guildShows = allShows.filter(
      (show) =>
        show.destinations.some((d) => d.guildId === guildId) &&
        show.episodes.some((e) => !e.messageSent),
    );

    const embed = getUpcomingEpisodesEmbed(guildShows, 1);

    console.info(
      `Morning summary: guild ${guildId} — digest of ${guildShows.length} show(s) to ${channelIds.length} channel(s)`,
    );

    for (const channelId of channelIds) {
      total += 1;
      try {
        await sendDiscordEmbed(channelId, embed, token);
        sent += 1;
      } catch (e) {
        console.error(`Error sending morning summary to ${channelId}`, e);
      }
    }
  }

  console.info(`Morning summary: sent to ${sent}/${total} destination(s)`);
}
