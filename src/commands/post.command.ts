import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import type { SeriesExtendedRecord } from "../interfaces/tvdb.generated.js";
import { editInteractionResponse } from "../lib/discord.js";
import { getEnv } from "../lib/env.js";
import {
  getChannelOption,
  getGuildId,
  getStringOption,
} from "../lib/interactionOptions.js";
import {
  buildShowEmbed,
  createForumThread,
  deleteChannel,
  pinMessage,
} from "../lib/messages.js";
import { ProgressMessageBuilder, StepStatus } from "../lib/progressMessages.js";
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
      description:
        'Create a forum post for a show. Requires "Manage Channels" permission.',
      default_member_permissions: "16",
      contexts: [InteractionContextType.Guild],
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
          description:
            "Destination Discord forum for show post (defaults to value defined in `/setting default_forum`)",
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

    const guildId = getGuildId(interaction);
    if (guildId == null) {
      await editInteractionResponse(token, {
        content: "This command can only be used in a server.",
      });
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
        content: "You can only create 10 posts at a time",
      });
      return;
    }

    const imdbIdString = imdbIds.map((s) => `\`${s}\``).join(", ");

    const progress = new ProgressMessageBuilder(token)
      .addStep(`Checking for existing forum posts with ID(s) ${imdbIdString}`)
      .addStep("Fetching show data")
      .addStep("Creating forum post")
      .addStep("Saving show to DB")
      .addStep("Fetching upcoming episodes");

    try {
      // Resolve forum channel
      let forumId = forumOption;
      if (forumId == null) {
        forumId = await getDefaultForum(guildId);
      }
      if (forumId == null) {
        await editInteractionResponse(token, {
          content:
            "No default forum configured. Use `/setting default_forum channel:<channel>` to set one, or provide a `forum` option.",
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

      if (found.length === 0) {
        progress.setCurrentStatus(StepStatus.ERROR);
        await editInteractionResponse(token, {
          content: `${progress.toString()}\n\nError: No show found with IMDB ID(s) ${imdbIdString}`,
        });
        return;
      }

      // Step 3: Creating forum post
      await progress.sendNextStep();
      const created: {
        imdbId: string;
        series: SeriesExtendedRecord;
        threadId: string;
      }[] = [];

      for (const { imdbId, series } of found) {
        let createdThreadId: string | null = null;
        try {
          const embed = buildShowEmbed(imdbId, series);

          const { threadId, messageId } = await createForumThread(
            forumId,
            series.name,
            embed,
            discordToken,
          );
          createdThreadId = threadId;

          await pinMessage(threadId, messageId, discordToken);

          created.push({ imdbId, series, threadId });
        } catch (error) {
          messages.push(`Error creating post for \`${imdbId}\``);
          console.error(error);
          if (createdThreadId !== null) {
            try {
              await deleteChannel(createdThreadId, discordToken);
              console.info(
                `Rolled back orphan thread ${createdThreadId} for ${imdbId}`,
              );
            } catch (deleteError) {
              console.error(
                `Failed to delete orphan thread ${createdThreadId} for ${imdbId}:`,
                deleteError,
              );
            }
          }
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
          console.info(
            `[New Subscription] ${series.name} (${imdbId}) ${threadId}`,
          );
          const show = await createNewSubscription(
            imdbId,
            series.id,
            series.name,
            { guildId, channelId: threadId, forumId },
          );

          saved.push({ imdbId, series, show });
          messages.push(
            `Created post for \`${series.name}\` (${imdbId}) - <#${threadId}>`,
          );
          console.info(`Added show ${series.name} (${imdbId})`);
        } catch (error) {
          messages.push(`Error saving show \`${imdbId}\` to DB`);
          console.error(error);
          try {
            await deleteChannel(threadId, discordToken);
            console.info(
              `Rolled back orphan thread ${threadId} for ${imdbId} after DB save failure`,
            );
          } catch (deleteError) {
            console.error(
              `Failed to delete orphan thread ${threadId} for ${imdbId}:`,
              deleteError,
            );
          }
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
        content: `${progress.toString()}\n\nAn error occurred while creating posts.`,
      });
    }
  }
}
