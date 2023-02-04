import { ApplicationCommandOptionChoiceData, AutocompleteInteraction, ChatInputCommandInteraction, PermissionFlagsBits, SlashCommandBuilder } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { App } from '../app'
import { getSeriesByImdbId, getSeriesByName } from '../lib/tvdb'
import { buildShowEmbed } from '../lib/messages'
import { Prisma } from '@prisma/client'

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName('search')
      .setDescription('Link a show to a channel for notifications.')
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
      .addStringOption(option => option.setName('query')
        .setDescription('Query to search for. Can be an IMDB ID or a show name')
        .setMinLength(1)
        .setAutocomplete(true)
        .setRequired(true)
      ),
  },
  async execute(app: App, interaction: ChatInputCommandInteraction) {
    const query = interaction.options.getString('query', true)

    let imdbId = query.toLowerCase().startsWith('tt') ? query : undefined

    if (imdbId !== undefined) {
      const series = await getSeriesByImdbId(imdbId)

      if (series === undefined) return await interaction.editReply(`No show found with IMDB ID \`${imdbId}\``)

      const show = await client.show.findUnique({
        where: {
          imdbId
        },
        select: {
          destinations: true
        }
      })

      return await interaction.editReply({ embeds: [await buildShowEmbed(imdbId, series, show?.destinations ?? [])] })
    }

    const series = await getSeriesByName(query)

    if (series == null) return await interaction.editReply('Show not found')

    imdbId = series.remoteIds.find(r => r.type == 2)?.id

    if (imdbId == null) return await interaction.editReply('Show not found')

    return await interaction.editReply({ embeds: [await buildShowEmbed(imdbId, series, [])] })
  },
  async autocomplete(app: App, interaction: AutocompleteInteraction) {
    const focusedValue = interaction.options.getFocused()

    if (focusedValue === undefined) return

    const where: Prisma.ShowWhereInput = focusedValue.toLocaleLowerCase().startsWith('tt') ?
      {
        imdbId: {
          startsWith: focusedValue,
          mode: 'insensitive'
        }
      }
      :
      {
        name: {
          startsWith: focusedValue,
          mode: 'insensitive'
        }
      }

    const data = await client.show.findMany({
      where,
      orderBy: {
        name: 'asc'
      },
      select: {
        name: true,
        imdbId: true
      },
      take: 25
    })

    const choices = data.map((item): ApplicationCommandOptionChoiceData<string | number> => {
      const { name, imdbId } = item
      return ({ name: `${name} - (${name})`, value: imdbId })
    })

    await interaction.respond(choices)
  }
}
