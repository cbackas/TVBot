import { getEnv } from "./env.js";
import { sendDiscordEmbed } from "./messages.js";
import { getGlobalDestinations } from "./settingsManager.js";
import { getAllShows } from "./shows.js";
import { getUpcomingEpisodesEmbed } from "./upcoming.js";

export async function sendMorningSummary(): Promise<void> {
  const destinations = await getGlobalDestinations("morning_summary");
  if (destinations.length === 0) {
    console.info(
      "Morning summary: no `morning_summary` destinations configured — nothing to send. Use `/setting morning_summary add`.",
    );
    return;
  }

  const allShows = await getAllShows();
  const showsWithUnsentEpisodes = allShows.filter((show) =>
    show.episodes.some((e) => !e.messageSent),
  );

  const embed = getUpcomingEpisodesEmbed(showsWithUnsentEpisodes, 1);
  const token = getEnv("DISCORD_TOKEN");

  console.info(
    `Morning summary: sending digest of ${showsWithUnsentEpisodes.length} show(s) to ${destinations.length} destination(s)`,
  );

  let sent = 0;
  for (const dest of destinations) {
    try {
      await sendDiscordEmbed(dest.channelId, embed, token);
      sent += 1;
    } catch (e) {
      console.error(`Error sending morning summary to ${dest.channelId}`, e);
    }
  }

  console.info(
    `Morning summary: sent to ${sent}/${destinations.length} destination(s)`,
  );
}
