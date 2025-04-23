import { type Prisma } from "prisma-client/client.ts"
import {
  type ApplicationCommandOptionChoiceData,
  type AutocompleteInteraction,
} from "npm:discord.js"
import client from "lib/prisma.ts"

export async function showSearchAutocomplete(
  interaction: AutocompleteInteraction,
): Promise<void> {
  const focusedValue = interaction.options.getFocused()

  if (focusedValue === undefined) return

  const where: Prisma.ShowWhereInput =
    focusedValue.toLocaleLowerCase().startsWith("tt")
      ? {
        imdbId: {
          startsWith: focusedValue,
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
      return ({ name: `${name} - (${name})`, value: imdbId })
    },
  )

  await interaction.respond(choices)
}
