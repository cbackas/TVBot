import {
  type AutocompleteInteraction,
  type ChatInputCommandInteraction,
  InteractionContextType,
  SlashCommandBuilder,
} from "discord.js"
import client from "lib/prisma.ts"
import { type Destination } from "prisma-client/client.ts"
import { type CommandV2 } from "interfaces/command.ts"
import {
  getSeriesByImdbId,
  getSeriesByTvdbId,
  getSeriesCandidates,
  type SeriesCandidate,
} from "lib/tvdb.ts"
import { buildShowEmbed } from "lib/messages.ts"
import { showSearchAutocomplete } from "lib/autocomplete.ts"
import { parseIMDBIds } from "lib/util.ts"
import { type CarouselPage, sendCarousel } from "lib/carousel.ts"
import { type SeriesExtendedRecord } from "interfaces/tvdb.generated.ts"

/** Channels a show is linked to, or none if the show isn't tracked. */
async function getDestinations(imdbId: string): Promise<Destination[]> {
  const show = await client.show.findUnique({
    where: { imdbId },
    select: { destinations: true },
  })
  return show?.destinations ?? []
}

/**
 * Builds the page shown for a show, with the channels it is linked to
 * @param fallbackYear year to use if the show record doesn't have one
 */
async function buildSeriesPage(
  imdbId: string,
  series: SeriesExtendedRecord,
  fallbackYear?: string,
): Promise<CarouselPage> {
  const year = [series.year, series.firstAired?.slice(0, 4), fallbackYear]
    .find((y) => y != null && y !== "")

  // some TVDB names already end in a year, like "The Count of Monte Cristo (1965)"
  const nameHasYear = /\(\d{4}\)\s*$/.test(series.name)
  const title = year != null && !nameHasYear
    ? `${series.name} (${year})`
    : series.name
  const embed = buildShowEmbed(imdbId, series, await getDestinations(imdbId))

  return {
    label: title,
    // tells apart shows with the same name and year
    description: series.latestNetwork?.name,
    // the year is only added here so /link and /post embeds stay as they are
    embed: { ...embed, title },
  }
}

/**
 * Builds the carousel page for one name search match
 * @returns the page, or undefined if the show can't be loaded or has no IMDB id
 */
async function buildCandidatePage(
  candidate: SeriesCandidate,
): Promise<CarouselPage | undefined> {
  const series = await getSeriesByTvdbId(parseInt(candidate.tvdb_id))
  if (series == null) return undefined

  const imdbId = series.remoteIds?.find((r) => r.type === 2)?.id
  if (imdbId == null) return undefined

  return await buildSeriesPage(imdbId, series, candidate.year)
}

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName("search")
      .setDescription("Look up a show and see its details and linked channels.")
      .setContexts(InteractionContextType.Guild)
      .addStringOption((option) =>
        option.setName("query")
          .setDescription(
            "Query to search for. Can be an IMDB ID, an IMDB link or a show name",
          )
          .setMinLength(1)
          .setAutocomplete(true)
          .setRequired(true)
      )
      .addIntegerOption((option) =>
        option.setName("year")
          .setDescription("Only show matches first aired in this year")
          .setMinValue(1900)
          .setMaxValue(2100)
      ),
  },
  async executeCommand(interaction: ChatInputCommandInteraction) {
    const query = interaction.options.getString("query", true).trim()

    // the query may be an IMDB ID or an IMDB link; anything else is a show name
    const imdbId: string | undefined = parseIMDBIds(query.toLowerCase())[0]

    if (imdbId !== undefined) {
      const series = await getSeriesByImdbId(imdbId)

      if (series === undefined) {
        return await interaction.editReply(
          `No show found with IMDB ID \`${imdbId}\``,
        )
      }

      return await sendCarousel(interaction, [
        await buildSeriesPage(imdbId, series),
      ])
    }

    // an IMDB id or link already points at one show, so the year only narrows name searches
    const year = interaction.options.getInteger("year") ?? undefined
    const candidates = await getSeriesCandidates(query, year)

    if (candidates.length === 0) {
      return await interaction.editReply("Show not found")
    }

    // the search results don't reliably say which shows have an IMDB id, so
    // load every match now and drop the ones without, keeping the page count right
    const pages = (await Promise.all(candidates.map(buildCandidatePage)))
      .filter((page) => page != null)

    if (pages.length === 0) {
      return await interaction.editReply("Show not found")
    }

    return await sendCarousel(interaction, pages)
  },
  async executeAutoComplete(interaction: AutocompleteInteraction) {
    await showSearchAutocomplete(interaction)
  },
}
