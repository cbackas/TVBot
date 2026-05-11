import {
  type APIApplicationCommandInteraction,
  type APIMessageComponentInteraction,
  ApplicationCommandOptionType,
  ButtonStyle,
  ChannelType,
  ComponentType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { InteractionResponseType } from "discord-interactions";
import { editInteractionResponse } from "../lib/discord.js";
import { getChannelOption, getSubcommand } from "../lib/interactionOptions.js";
import {
  getAllShows,
  pruneUnsubscribedShows,
  removeSubscription,
} from "../lib/shows.js";
import type { Command } from "./index.js";

const MAX_OPTIONS = 25;

function buildPageComponents(
  allOptions: { label: string; value: string }[],
  page: number,
) {
  const totalPages = Math.ceil(allOptions.length / MAX_OPTIONS);
  const pageOptions = allOptions.slice(
    page * MAX_OPTIONS,
    (page + 1) * MAX_OPTIONS,
  );

  const components: object[] = [
    {
      type: ComponentType.ActionRow,
      components: [
        {
          type: ComponentType.StringSelect,
          custom_id: "unlink_shows_menu",
          placeholder: "Nothing selected",
          min_values: 1,
          max_values: pageOptions.length,
          options: pageOptions,
        },
      ],
    },
  ];

  if (totalPages > 1) {
    components.push({
      type: ComponentType.ActionRow,
      components: [
        {
          type: ComponentType.Button,
          style: ButtonStyle.Secondary,
          label: "Previous",
          custom_id: `unlink_shows_menu:prev:${page - 1}`,
          disabled: page === 0,
        },
        {
          type: ComponentType.Button,
          style: ButtonStyle.Secondary,
          label: "Next",
          custom_id: `unlink_shows_menu:next:${page + 1}`,
          disabled: page === totalPages - 1,
        },
      ],
    });
  }

  return { components, totalPages };
}

export default class UnlinkCommand implements Command {
  public readonly name = "unlink";
  public readonly selectMenuIds = ["unlink_shows_menu"];

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "unlink" as const,
      description: "Unlink shows from a channel",
      default_member_permissions: "16",
      options: [
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "here",
          description: "Unlink from the current channel",
        },
        {
          type: ApplicationCommandOptionType.Subcommand,
          name: "channel",
          description: "Unlink from a specific channel",
          options: [
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

    let channelId: string | null = null;
    if (sub === "here") {
      channelId = interaction.channel?.id ?? null;
    } else if (sub === "channel") {
      channelId = getChannelOption(interaction, "channel");
    }

    if (channelId == null) {
      await editInteractionResponse(interaction.token, {
        content: "Invalid channel",
      });
      return;
    }

    const allShows = await getAllShows();
    const showsInChannel = allShows.filter((show) =>
      show.destinations.some((d) => d.channelId === channelId),
    );

    if (showsInChannel.length === 0) {
      await editInteractionResponse(interaction.token, {
        content: "This channel has no shows linked to it.",
      });
      return;
    }

    const allOptions = showsInChannel
      .map((s) => ({ label: s.name, value: s.imdbId }))
      .sort((a, b) => a.label.localeCompare(b.label));

    const { components, totalPages } = buildPageComponents(allOptions, 0);

    let content = `Select the shows that you'd like to unlink from <#${channelId}>`;
    if (totalPages > 1) {
      content += ` (Page 1 of ${totalPages})`;
    }

    await editInteractionResponse(interaction.token, {
      content,
      components,
    });
  }

  async componentHandler(
    interaction: APIMessageComponentInteraction,
  ): Promise<Response> {
    const customId = interaction.data.custom_id;
    const channelId = interaction.message?.content?.match(/<#([0-9]+)>/)?.[1];

    if (channelId == null) {
      return Response.json({
        type: InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: { content: "Failed to find channel" },
      });
    }

    // Handle pagination buttons
    if (
      customId.startsWith("unlink_shows_menu:prev:") ||
      customId.startsWith("unlink_shows_menu:next:")
    ) {
      const targetPage = parseInt(customId.split(":")[2], 10);

      const allShows = await getAllShows();
      const showsInChannel = allShows.filter((show) =>
        show.destinations.some((d) => d.channelId === channelId),
      );

      const allOptions = showsInChannel
        .map((s) => ({ label: s.name, value: s.imdbId }))
        .sort((a, b) => a.label.localeCompare(b.label));

      const { components, totalPages } = buildPageComponents(
        allOptions,
        targetPage,
      );

      return Response.json({
        type: InteractionResponseType.UPDATE_MESSAGE,
        data: {
          content: `Select the shows that you'd like to unlink from <#${channelId}> (Page ${targetPage + 1} of ${totalPages})`,
          components,
        },
      });
    }

    // Handle select menu submission
    const values = "values" in interaction.data ? interaction.data.values : [];

    const success: string[] = [];
    const failed: string[] = [];
    for (const imdbId of values) {
      try {
        const show = await removeSubscription(imdbId, channelId);
        success.push(show.name);
      } catch (error) {
        failed.push(imdbId);
        console.error(`Failed to unlink ${imdbId}:`, error);
      }
    }

    await pruneUnsubscribedShows();

    const content = [
      `Unlinked ${success.length} shows from <#${channelId}>:`,
      ...success.map((s) => `- ${s}`),
    ].join("\n");

    return Response.json({
      type: InteractionResponseType.UPDATE_MESSAGE,
      data: {
        content,
        components: [],
      },
    });
  }
}
