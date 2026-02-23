import type { APIApplicationCommandInteraction } from "discord-api-types/v10";
import type { Show } from "../database/types.js";
import type { SeriesExtendedRecord } from "../interfaces/tvdb.generated.js";
import { deferWithWork, editInteractionResponse } from "../lib/discord.js";
import { getEnv } from "../lib/env.js";
import {
  getChannelOption,
  getStringOption,
  getSubcommand,
} from "../lib/interactionOptions.js";
import { buildShowEmbed, sendDiscordEmbed } from "../lib/messages.js";
import { ProgressMessageBuilder } from "../lib/progressMessages.js";
import {
  createNewSubscription,
  getAllShows,
  updateEpisodes,
} from "../lib/shows.js";
import { getSeriesByImdbId } from "../lib/tvdb.js";
import { parseIMDBIds } from "../lib/util.js";
import type { Command } from "./index.js";

export default class LinkCommand implements Command {
  public readonly name = "link";

  async handler(
    interaction: APIApplicationCommandInteraction,
  ): Promise<Response> {
    const sub = getSubcommand(interaction);
    const imdbIdInput = getStringOption(interaction, "imdb_id") ?? "";
    const token = interaction.token;
    const discordToken = getEnv("DISCORD_TOKEN");

    let channelId: string | null = null;
    if (sub === "here") {
      channelId = interaction.channel?.id ?? null;
    } else if (sub === "channel") {
      channelId = getChannelOption(interaction, "channel");
    }

    if (channelId == null) {
      return Response.json({
        type: 4,
        data: { content: "Invalid channel" },
      });
    }

    const imdbIds = parseIMDBIds(imdbIdInput);

    if (imdbIds.length === 0) {
      return Response.json({
        type: 4,
        data: { content: "No valid IMDB IDs provided" },
      });
    }

    if (imdbIds.length > 10) {
      return Response.json({
        type: 4,
        data: { content: "You can only create 10 links at a time" },
      });
    }

    const targetChannelId = channelId;

    return deferWithWork(
      (async () => {
        const progress = new ProgressMessageBuilder(token)
          .addStep("Check for existing show subscriptions")
          .addStep("Searching for shows with IMDB ID(s) ...")
          .addStep("Linking shows to channel")
          .addStep("Fetching upcoming episodes");

        try {
          const messages: string[] = [];

          // Step 1: Check for existing show subscriptions
          await progress.sendNextStep();
          const allShows = await getAllShows();

          // Step 2: Searching for shows
          await progress.sendNextStep();
          const found: { imdbId: string; series: SeriesExtendedRecord }[] = [];

          for (const imdbId of imdbIds) {
            const series = await getSeriesByImdbId(imdbId);
            if (series === undefined) {
              messages.push(`No show found with IMDB ID \`${imdbId}\``);
              continue;
            }

            const existingShow = allShows.find((s) => s.imdbId === imdbId);
            const alreadyLinked = existingShow?.destinations.some(
              (d) => d.channelId === targetChannelId,
            );

            if (alreadyLinked) {
              messages.push(
                `Show \`${series.name}\` is already linked to <#${targetChannelId}>`,
              );
              continue;
            }

            found.push({ imdbId, series });
          }

          // Step 3: Linking shows to channel
          await progress.sendNextStep();
          const linked: {
            imdbId: string;
            series: SeriesExtendedRecord;
            show: Show;
          }[] = [];

          for (const { imdbId, series } of found) {
            try {
              console.info(
                `[New Subscription] ${series.name} (${imdbId}) ${targetChannelId}`,
              );
              const show = await createNewSubscription(
                imdbId,
                series.id,
                series.name,
                { channelId: targetChannelId, forumId: null },
              );
              linked.push({ imdbId, series, show });
              messages.push(`Linked show \`${series.name}\` (${imdbId})`);
            } catch (error) {
              messages.push(
                `Failed to link show \`${series.name}\` (${imdbId})`,
              );
              console.error(error);
            }
          }

          // Step 4: Fetching upcoming episodes
          await progress.sendNextStep();

          for (const { imdbId, series, show } of linked) {
            try {
              await updateEpisodes(show.imdbId, show.tvdbId, series);
              await sendDiscordEmbed(
                targetChannelId,
                buildShowEmbed(imdbId, series, show.destinations),
                discordToken,
              );
            } catch (error) {
              console.error(
                `Error updating episodes for ${series.name}:`,
                error,
              );
            }
          }

          await editInteractionResponse(token, {
            content:
              progress.toString() +
              `\n\nLinked show(s) to <#${targetChannelId}>:\n${messages.join("\n")}`,
          });
        } catch (error) {
          console.error("Error in link command:", error);
          await editInteractionResponse(token, {
            content:
              progress.toString() +
              "\n\nAn error occurred while linking shows.",
          });
        }
      })(),
    );
  }
}
