import {
  type AutocompleteInteraction,
  type ChatInputCommandInteraction,
  InteractionContextType,
  SlashCommandBuilder,
} from "discord.js"
import client from "lib/prisma.ts"
import { type Destination } from "prisma-client/client.ts"
import { type CommandV2 } from "interfaces/command.ts"
import { getSeriesByImdbId, getSeriesByName } from "lib/tvdb.ts"
import { buildShowEmbed } from "lib/messages.ts"
import { showSearchAutocomplete } from "lib/autocomplete.ts"
import { parseIMDBIds } from "lib/util.ts"

/** Channels a show is linked to, or none if the show isn't tracked. */
async function getDestinations(imdbId: string): Promise<Destination[]> {
  const show = await client.show.findUnique({
    where: { imdbId },
    select: { destinations: true },
  })
  return show?.destinations ?? []
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
      ),
  },
  async executeCommand(interaction: ChatInputCommandInteraction) {
    const query = interaction.options.getString("query", true).trim()

    // the query may be an IMDB ID or an IMDB link; anything else is a show name
    let imdbId: string | undefined = parseIMDBIds(query.toLowerCase())[0]

    if (imdbId !== undefined) {
      const series = await getSeriesByImdbId(imdbId)

      if (series === undefined) {
        return await interaction.editReply(
          `No show found with IMDB ID \`${imdbId}\``,
        )
      }

      return await interaction.editReply({
        embeds: [
          buildShowEmbed(imdbId, series, await getDestinations(imdbId)),
        ],
      })
    }

    const series = await getSeriesByName(query)

    if (series == null) return await interaction.editReply("Show not found")

    imdbId = series.remoteIds.find((r) => r.type === 2)?.id

    if (imdbId == null) return await interaction.editReply("Show not found")

    return await interaction.editReply({
      embeds: [buildShowEmbed(imdbId, series, await getDestinations(imdbId))],
    })
  },
  async executeAutoComplete(interaction: AutocompleteInteraction) {
    await showSearchAutocomplete(interaction)
  },
}
