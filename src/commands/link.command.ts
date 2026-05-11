import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import type { Show } from "../database/types.js";
import type { SeriesExtendedRecord } from "../interfaces/tvdb.generated.js";
import { editInteractionResponse } from "../lib/discord.js";
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

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "link" as const,
      description: "Link a show to a channel for notifications",
      default_member_permissions: "16",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "here",
          description: "Link to the current channel",
          options: [
            {
              type: ApplicationCommandOptionType.String,
              name: "imdb_id",
              description: "IMDB ID(s), comma-separated",
              required: true,
              min_length: 9,
            },
          ],
        },
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "channel",
          description: "Link to a specific channel",
          options: [
            {
              type: ApplicationCommandOptionType.String,
              name: "imdb_id",
              description: "IMDB ID(s), comma-separated",
              required: true,
              min_length: 9,
            },
            {
              type: ApplicationCommandOptionType.Channel,
              name: "channel",
              description: "Target channel",
              required: true,
              channel_types: [ChannelType.GuildText],
            },
          ],
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const sub = getSubcommand(interaction);
    const imdbIdInput = getStringOption(interaction, "imdb_id") ?? "";
    const token = interaction.token;
    const discordToken = getEnv("DISCORD_TOKEN");

    const THREAD_TYPES = new Set<number>([
      ChannelType.AnnouncementThread,
      ChannelType.PublicThread,
      ChannelType.PrivateThread,
    ]);

    let channelId: string | null = null;
    let parentForumId: string | null = null;
    if (sub === "here") {
      const channel = interaction.channel;
      channelId = channel?.id ?? null;
      if (
        channel != null &&
        THREAD_TYPES.has(channel.type) &&
        "parent_id" in channel
      ) {
        parentForumId = channel.parent_id ?? null;
      }
    } else if (sub === "channel") {
      channelId = getChannelOption(interaction, "channel");
    }

    if (channelId == null) {
      await editInteractionResponse(token, { content: "Invalid channel" });
      return;
    }

    const imdbIds = parseIMDBIds(imdbIdInput);

    if (imdbIds.length === 0) {
      await editInteractionResponse(token, {
        content: "No valid IMDB IDs provided",
      });
      return;
    }

    if (imdbIds.length > 10) {
      await editInteractionResponse(token, {
        content: "You can only create 10 links at a time",
      });
      return;
    }

    const targetChannelId = channelId;
    const imdbIdString = imdbIds.map((s) => `\`${s}\``).join(", ");

    const progress = new ProgressMessageBuilder(token)
      .addStep("Check for existing show subscriptions")
      .addStep(`Searching for shows with IMDB ID(s) ${imdbIdString}`)
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

      if (found.length === 0) {
        await editInteractionResponse(token, {
          content: `${progress.toString()}\n\nError: No shows found with IMDB ID(s) ${imdbIdString}`,
        });
        return;
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
            { channelId: targetChannelId, forumId: parentForumId },
          );
          linked.push({ imdbId, series, show });
          messages.push(`Linked show \`${series.name}\` (${imdbId})`);
        } catch (error) {
          messages.push(`Failed to link show \`${series.name}\` (${imdbId})`);
          console.error(error);
        }
      }

      // Step 4: Fetching upcoming episodes
      await progress.sendNextStep();

      for (const { imdbId, series, show } of linked) {
        try {
          await updateEpisodes(show.imdbId, show.tvdbId, series);
        } catch (error) {
          console.error(`Error updating episodes for ${series.name}:`, error);
        }

        try {
          await sendDiscordEmbed(
            targetChannelId,
            buildShowEmbed(imdbId, series, show.destinations),
            discordToken,
            `Linked \`${series.name}\` to <#${targetChannelId}>`,
          );
        } catch (error) {
          console.error(
            `Error sending embed for ${series.name} to channel ${targetChannelId}:`,
            error,
          );
        }
      }

      await progress.sendNextStep();

      await editInteractionResponse(token, {
        content:
          progress.toString() +
          `\n\nLinked show(s) to <#${targetChannelId}>:\n${messages.join("\n")}`,
      });
    } catch (error) {
      console.error("Error in link command:", error);
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\nAn error occurred while linking shows.`,
      });
    }
  }
}
