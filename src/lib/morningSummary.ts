import { getEnv } from "./env.js";
import { sendDiscordEmbed } from "./messages.js";
import { getGlobalDestinations } from "./settingsManager.js";
import { getAllShows } from "./shows.js";
import { getUpcomingEpisodesEmbed } from "./upcoming.js";

export async function sendMorningSummary(): Promise<void> {
  const allShows = await getAllShows();
  const showsWithUnsentEpisodes = allShows.filter((show) =>
    show.episodes.some((e) => !e.messageSent),
  );

  const embed = getUpcomingEpisodesEmbed(showsWithUnsentEpisodes, 1);

  const token = getEnv("DISCORD_TOKEN");
  const destinations = await getGlobalDestinations("morning_summary");
  for (const dest of destinations) {
    try {
      await sendDiscordEmbed(dest.channelId, embed, token);
    } catch (e) {
      console.error(`Error sending morning summary to ${dest.channelId}`, e);
    }
  }
}
