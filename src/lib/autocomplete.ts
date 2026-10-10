import { type Prisma } from "prisma-client/client.ts"
import {
  type ApplicationCommandOptionChoiceData,
  type AutocompleteInteraction,
} from "discord.js"
import client from "lib/prisma.ts"
import { parseIMDBIds } from "lib/util.ts"

export async function showSearchAutocomplete(
  interaction: AutocompleteInteraction,
): Promise<void> {
  const focusedValue = interaction.options.getFocused()

  if (focusedValue === undefined) return

  // the input may be a (partial) IMDB ID or IMDB link; otherwise match by name
  const [parsedId] = parseIMDBIds(focusedValue.trim().toLowerCase())

  const where: Prisma.ShowWhereInput = parsedId != null && parsedId !== ""
    ? {
      imdbId: {
        startsWith: parsedId,
        mode: "insensitive",
      },
    }
    : {
      name: {
        startsWith: focusedValue,
        mode: "insensitive",
      },
    }

  const data = await client.show.findMany({
    where,
    orderBy: {
      name: "asc",
    },
    select: {
      name: true,
      imdbId: true,
    },
    take: 25,
  })

  const choices = data.map(
    (item): ApplicationCommandOptionChoiceData<string | number> => {
      const { name, imdbId } = item
      return ({
        // Discord caps choice names at 100 characters
        name: `${name} (${imdbId})`.slice(0, 100),
        value: imdbId,
      })
    },
  )

  await interaction.respond(choices)
}
