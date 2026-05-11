import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import type { SeriesExtendedRecord } from "../interfaces/tvdb.generated.js";
import { editInteractionResponse } from "../lib/discord.js";
import { getEnv } from "../lib/env.js";
import {
  getChannelOption,
  getStringOption,
} from "../lib/interactionOptions.js";
import {
  buildShowEmbed,
  createForumThread,
  pinMessage,
} from "../lib/messages.js";
import { ProgressMessageBuilder } from "../lib/progressMessages.js";
import { getDefaultForum } from "../lib/settingsManager.js";
import {
  createNewSubscription,
  getAllShows,
  updateEpisodes,
} from "../lib/shows.js";
import { getSeriesByImdbId } from "../lib/tvdb.js";
import { parseIMDBIds } from "../lib/util.js";
import type { Command } from "./index.js";

export default class PostCommand implements Command {
  public readonly name = "post";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "post" as const,
      description: "Create a forum post for a show",
      default_member_permissions: "16",
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
          name: "forum",
          description: "Forum channel (defaults to configured forum)",
          required: false,
          channel_types: [ChannelType.GuildForum],
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const imdbIdInput = getStringOption(interaction, "imdb_id") ?? "";
    const forumOption = getChannelOption(interaction, "forum");
    const token = interaction.token;
    const discordToken = getEnv("DISCORD_TOKEN");

    const imdbIds = parseIMDBIds(imdbIdInput);

    if (imdbIds.length === 0) {
      await editInteractionResponse(token, {
        content: "No valid IMDB IDs provided",
      });
      return;
    }

    if (imdbIds.length > 10) {
      await editInteractionResponse(token, {
        content: "You can only create 10 posts at a time",
      });
      return;
    }

    const progress = new ProgressMessageBuilder(token)
      .addStep("Checking for existing forum posts with ID(s) ...")
      .addStep("Fetching show data")
      .addStep("Creating forum post")
      .addStep("Saving show to DB")
      .addStep("Fetching upcoming episodes");

    try {
      // Resolve forum channel
      let forumId = forumOption;
      if (forumId == null) {
        forumId = await getDefaultForum();
      }
      if (forumId == null) {
        await editInteractionResponse(token, {
          content:
            "No TV forum configured. Use `/setting tv_forum <channel>` to set the default TV forum, or provide a forum channel with the `forum` option.",
        });
        return;
      }

      const messages: string[] = [];

      // Step 1: Checking for existing forum posts
      await progress.sendNextStep();
      const allShows = await getAllShows();
      const idsToProcess: string[] = [];

      for (const imdbId of imdbIds) {
        const existingShow = allShows.find((s) => s.imdbId === imdbId);
        const alreadyPosted = existingShow?.destinations.some(
          (d) => d.forumId === forumId,
        );

        if (alreadyPosted) {
          messages.push(`A post for \`${imdbId}\` already exists`);
        } else {
          idsToProcess.push(imdbId);
        }
      }

      if (idsToProcess.length === 0) {
        await editInteractionResponse(token, {
          content: `All show(s) already have a post in <#${forumId}>`,
        });
        return;
      }

      // Step 2: Fetching show data
      await progress.sendNextStep();
      const found: { imdbId: string; series: SeriesExtendedRecord }[] = [];

      for (const imdbId of idsToProcess) {
        const series = await getSeriesByImdbId(imdbId);
        if (series === undefined) {
          messages.push(`No show found with IMDB ID \`${imdbId}\``);
          continue;
        }
        found.push({ imdbId, series });
      }

      // Step 3: Creating forum post
      await progress.sendNextStep();
      const created: {
        imdbId: string;
        series: SeriesExtendedRecord;
        threadId: string;
      }[] = [];

      for (const { imdbId, series } of found) {
        try {
          const embed = buildShowEmbed(imdbId, series);

          const { threadId, messageId } = await createForumThread(
            forumId,
            series.name,
            embed,
            discordToken,
          );

          await pinMessage(threadId, messageId, discordToken);

          created.push({ imdbId, series, threadId });
        } catch (error) {
          messages.push(`Error creating post for \`${imdbId}\``);
          console.error(error);
        }
      }

      // Step 4: Saving show to DB
      await progress.sendNextStep();
      const saved: {
        imdbId: string;
        series: SeriesExtendedRecord;
        show: { imdbId: string; tvdbId: number };
      }[] = [];

      for (const { imdbId, series, threadId } of created) {
        try {
          const show = await createNewSubscription(
            imdbId,
            series.id,
            series.name,
            { channelId: threadId, forumId },
          );

          saved.push({ imdbId, series, show });
          messages.push(
            `Created post for \`${series.name}\` (${imdbId}) - <#${threadId}>`,
          );
          console.info(`Added show ${series.name} (${imdbId})`);
        } catch (error) {
          messages.push(`Error saving show \`${imdbId}\` to DB`);
          console.error(error);
        }
      }

      // Step 5: Fetching upcoming episodes
      await progress.sendNextStep();

      for (const { series, show } of saved) {
        try {
          await updateEpisodes(show.imdbId, show.tvdbId, series);
        } catch (error) {
          console.error(`Error updating episodes for ${series.name}:`, error);
        }
      }

      await progress.sendNextStep();

      await editInteractionResponse(token, {
        content:
          progress.toString() +
          `\n\nCreating post(s) in <#${forumId}>:\n${messages.join("\n")}`,
      });
    } catch (error) {
      console.error("Error in post command:", error);
      await editInteractionResponse(token, {
        content:
          progress.toString() + "\n\nAn error occurred while creating posts.",
      });
    }
  }
}
