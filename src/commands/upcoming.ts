import {
  type AutocompleteInteraction,
  type ChatInputCommandInteraction,
  SlashCommandBuilder,
  SlashCommandSubcommandBuilder,
} from "npm:discord.js"
import client from "lib/prisma.ts"
import { type CommandV2 } from "interfaces/command.ts"
import { type App } from "app.ts"
import { getSeriesByImdbId } from "lib/tvdb.ts"
import { showSearchAutocomplete } from "lib/autocomplete.ts"
import { type Show } from "prisma-client/client.ts"
import { ProgressError } from "interfaces/error.ts"
import { getUpcomingEpisodesEmbed } from "lib/upcoming.ts"

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("upcoming")
      .setDescription("Get upcoming episodes")
      .setDMPermission(false),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName("all")
        .setDescription(
          "Get upcoming episodes this week for all tracked shows",
        ),
      new SlashCommandSubcommandBuilder()
        .setName("here")
        .setDescription("Get upcoming episodes for this channel"),
      new SlashCommandSubcommandBuilder()
        .setName("show")
        .setDescription("Get upcoming episodes for a show")
        .addStringOption((option) =>
          option.setName("query")
            .setDescription(
              "Search for a show saved in the DB. Use the autocomplete!",
            )
            .setAutocomplete(true)
        ),
    ],
  },
  async executeCommand(_app: App, interaction: ChatInputCommandInteraction) {
    const subCommand = interaction.options.getSubcommand()

    let s: Show[] = []

    try {
      switch (subCommand) {
        case "all":
          s = await getAllShows()
          break
        case "here":
          s = await getShowsHere(interaction.channelId)
          break
        case "show":
          s = [
            await getShowByImdbId(interaction.options.getString("query", true)),
          ]
          break
      }
    } catch (error) {
      if (error instanceof Error) {
        return await interaction.editReply(error.message)
      }
    }

    if (s == null || s.length === 0) {
      return await interaction.editReply("No shows found")
    }

    // const message = await getUpcomingEpisodesMessage(s, 7)
    const embed = await getUpcomingEpisodesEmbed(s, 7)

    return await interaction.editReply({
      content: "",
      embeds: [embed],
    })
  },
  async executeAutoComplate(_app: App, interaction: AutocompleteInteraction) {
    await showSearchAutocomplete(interaction)
  },
}

async function getAllShows(): Promise<Show[]> {
  const shows = await client.show.findMany({
    where: {
      episodes: {
        some: { messageSent: false },
      },
    },
  })

  if (shows.length === 0) throw new ProgressError("No shows found")

  return shows
}

/**
 * Get all shows that are linked to this channel
 */
async function getShowsHere(channelId: string): Promise<Show[]> {
  const shows = await client.show.findMany({
    where: {
      destinations: {
        some: { channelId },
      },
    },
  })

  if (shows.length === 0) {
    throw new ProgressError("No shows linked to this channel")
  }

  return shows
}

/**
 * Get the show by IMDB ID in the querys
 */
async function getShowByImdbId(query: string): Promise<Show> {
  // check that the query is an IMDB ID
  const imdbId = query.toLowerCase().startsWith("tt") ? query : undefined
  if (imdbId == null) throw new ProgressError("Invalid query")

  // turn IMDB ID into a series
  const series = await getSeriesByImdbId(imdbId)
  if (series == null) {
    throw new ProgressError(`No show found with IMDB ID \`${imdbId}\``)
  }

  const show = await client.show.findUnique({
    where: {
      imdbId,
    },
  })

  if (show == null) {
    throw new ProgressError(
      `${series.name} is not linked to any channels. Use \`/link\` or \`/post\` to subscribe a channel to episode notifications.`,
    )
  }

  return show
}
