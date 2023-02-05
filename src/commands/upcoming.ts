import { APIEmbed, APIEmbedField, AutocompleteInteraction, ChatInputCommandInteraction, SlashCommandBuilder, SlashCommandSubcommandBuilder } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { showSearchAutocomplete } from '../lib/autocomplete'
import moment from 'moment'
import { addLeadingZeros } from '../lib/util'
import { Show } from '@prisma/client'

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName('upcoming')
      .setDescription('Get upcoming episodes')
      .setDMPermission(false),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName('here')
        .setDescription('Get upcoming episodes for this channel'),
      new SlashCommandSubcommandBuilder()
        .setName('show')
        .setDescription('Get upcoming episodes for a show')
        .addStringOption(option => option.setName('query')
          .setDescription('Search for a show saved in the DB. Use the autocomplete!')
          .setAutocomplete(true))
    ]
  },
  async execute(_app: App, interaction: ChatInputCommandInteraction) {
    const subCommand = interaction.options.getSubcommand()

    let s: Show | Show[] | undefined

    if (subCommand === 'here') {
      /**
       * Get all shows that are linked to this channel
       */
      const shows = await client.show.findMany({
        where: {
          destinations: {
            some: {
              channelId: interaction.channelId
            }
          }
        }
      })

      if (shows.length === 0) return await interaction.editReply('No shows linked to this channel')

      s = shows
    } else {
      /**
       * Get the show by IMDB ID in the query
       */
      const query = interaction.options.getString('query', true)

      // check that the query is an IMDB ID
      let imdbId = query.toLowerCase().startsWith('tt') ? query : undefined
      if (imdbId == null) return await interaction.editReply('Invalid query')

      // turn IMDB ID into a series
      const series = await getSeriesByImdbId(imdbId)
      if (series === undefined) return await interaction.editReply(`No show found with IMDB ID \`${imdbId}\``)

      const show = await client.show.findUnique({
        where: {
          imdbId
        },
      })

      if (show == null) return await interaction.editReply(`${series.name} is not linked to any channels. Use \`/link\` or \`/post\` to subscribe a channel to episode notifications.`)

      s = show
    }

    // build embed with upcoming episodes as fields
    const embed: APIEmbed = {
      title: 'Upcoming episodes',
      fields: createEmbedFields(s)
    }

    return await interaction.editReply({ content: '', embeds: [embed] })
  },
  async autocomplete(_app: App, interaction: AutocompleteInteraction) {
    await showSearchAutocomplete(interaction)
  }
}

/**
 * Converts a show or list of shows into a list of embed fields
 * Each field contains the name of the show and a list of upcoming episodes
 * @param s Show or array of shows
 * @returns Array of embed fields for shows
 */
function createEmbedFields(s: Show | Show[]): APIEmbedField[] {
  const shows = Array.isArray(s) ? s : [s]

  // sort the shows by the air date of the first episode
  let showsSorted = shows.sort((a, b) => {
    // sort a show episodes by air date
    const aEpisodes = a.episodes.sort((a, b) => {
      const aDate = moment.utc(a.airDate)
      const bDate = moment.utc(b.airDate)
      return aDate.isBefore(bDate) ? -1 : 1
    })

    // sort b show episodes by air date
    const bEpisodes = b.episodes.sort((a, b) => {
      const aDate = moment.utc(a.airDate)
      const bDate = moment.utc(b.airDate)
      return aDate.isBefore(bDate) ? -1 : 1
    })

    return moment.utc(aEpisodes[0].airDate).isBefore(moment.utc(bEpisodes[0].airDate)) ? -1 : 1
  })

  // create embed fields from shows
  const fields: APIEmbedField[] = showsSorted.map(show => {
    return {
      name: show.name,
      value: show.episodes
        // filter out episodes that have already aired
        .filter(e => {
          const momentUTC = moment.utc(new Date())
          const airDateUTC = moment.utc(e.airDate)
          return airDateUTC.isAfter(momentUTC)
        })
        // sort episodes by air date
        .sort((a, b) => {
          const aDate = moment.utc(a.airDate)
          const bDate = moment.utc(b.airDate)
          return aDate.isBefore(bDate) ? -1 : 1
        })
        // map episodes to strings
        .map(e => {
          const airDate = moment(e.airDate)
          return `S${addLeadingZeros(e.season, 2)}E${addLeadingZeros(e.number, 2)} ${e.title} - <t:${moment.utc(e.airDate).unix()}:R>`
        })
        .join('\n')
    }
  })

  return fields
}