import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  ChannelType,
  ComponentType,
  InteractionContextType,
  MessageFlags,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import {
  editInteractionResponse,
  textDisplayComponents,
} from "../lib/discord.js";
import {
  getChannelOption,
  getSubcommand,
  getSubcommandGroup,
} from "../lib/interactionOptions.js";
import { getAllShows } from "../lib/shows.js";
import type { Command } from "./index.js";

export default class ListCommand implements Command {
  public readonly name = "list";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "list" as const,
      description: "List linked shows",
      default_member_permissions: "16",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.SubcommandGroup,
          name: "shows",
          description: "List shows",
          options: [
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "here",
              description: "Shows linked to this channel",
            },
            {
              type: ApplicationCommandOptionType.Subcommand,
              name: "channel",
              description: "Shows linked to a specific channel",
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
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const group = getSubcommandGroup(interaction);
    const sub = getSubcommand(interaction);

    if (group !== "shows") {
      await editInteractionResponse(interaction.token, {
        content: "Invalid subcommand group",
      });
      return;
    }

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
        content: `No shows linked to <#${channelId}>`,
      });
      return;
    }

    const sorted = showsInChannel.sort((a, b) => a.name.localeCompare(b.name));

    const lines = sorted.map((show) => {
      const destinations = show.destinations
        .map((d) => `<#${d.channelId}>`)
        .join(" ");
      return `**${show.name}** ${destinations}`;
    });

    const header = textDisplayComponents(`Shows in channel <#${channelId}>:`);
    // Container limit is 40 components; 1 for the container itself, 1 for the header
    const body = textDisplayComponents(lines.join("\n"), {
      chunk: true,
      maxComponents: 38,
    });
    const textDisplays = [...header, ...body];

    await editInteractionResponse(interaction.token, {
      components: [
        {
          type: ComponentType.Container,
          components: textDisplays,
        },
      ],
      flags: MessageFlags.IsComponentsV2,
    });
  }
}
