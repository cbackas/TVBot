import { ActionRowBuilder, AnySelectMenuInteraction, ChannelType, ChatInputCommandInteraction, PermissionFlagsBits, SlashCommandBuilder, SlashCommandSubcommandBuilder, StringSelectMenuBuilder, TextBasedChannel } from 'discord.js'
import client from '../lib/prisma'
import { CommandV2 } from '../interfaces/command'
import { App } from '../app'
import { ProgressError } from '../interfaces/error'
import { ProgressMessageBuilder } from '../lib/progressMessages'

export const command: CommandV2 = {
  slashCommand: {
    main: new SlashCommandBuilder()
      .setName('unlink')
      .setDescription('Unlink a show to a channel for notifications.')
      .setDMPermission(false)
      .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels),
    subCommands: [
      new SlashCommandSubcommandBuilder()
        .setName('here')
        .setDescription('Unlink a show to the current channel for notifications.'),
      new SlashCommandSubcommandBuilder()
        .setName('channel')
        .setDescription('Unlink a show to a channel for notifications.')
        .addChannelOption(option => option.setName('channel')
          .setDescription('The channel to unlink from announcements')
          .addChannelTypes(ChannelType.GuildText)
          .setRequired(true))
    ]
  },
  selectMenuIds: ['unlink_shows_menu'],
  async execute(app: App, interaction: ChatInputCommandInteraction | AnySelectMenuInteraction) {
    // handle menu interactions
    if (interaction.isAnySelectMenu()) {
      await client.show.updateMany({
        where: {
          imdbId: {
            in: interaction.values
          }
        },
        data: {
          destinations: {
            deleteMany: {
              where: {
                channelId: interaction.channelId
              }
            }
          }
        }
      })

      const values = interaction.values
      return await interaction.editReply({ content: `Unlinked ${values.length} shows from <#${interaction.channelId}>`, components: [] })
    }

    const subCommand = interaction.options.getSubcommand()

    const progress = new ProgressMessageBuilder()
      .addStep('Check for existing show subscription')
      .addStep('Fetching upcoming episodes')

    let channel: TextBasedChannel | undefined

    // the `here` subcommand links the show to the current channel
    if (subCommand == 'here' && interaction.channel !== null) {
      channel = interaction.channel
    }

    // the `channel` subcommand allows a user to specify a text channel
    if (subCommand == 'channel') {
      channel = interaction.options.getChannel('channel', true) as TextBasedChannel
    }

    // error if the channel didnt get set for some reason
    if (channel === undefined) {
      return await interaction.editReply('Invalid channel')
    }

    try {
      const showsInChannel = await client.show.findMany({
        where: {
          destinations: {
            some: {
              channelId: channel?.id
            }
          }
        },
        include: {
          destinations: true
        }
      })

      if (showsInChannel === null) return await interaction.editReply('This channel has no shows linked to it.')


      const row = new ActionRowBuilder<StringSelectMenuBuilder>()
        .addComponents(
          new StringSelectMenuBuilder()
            .setCustomId('unlink_shows_menu')
            .setPlaceholder('Nothing selected')
            .setMinValues(1)
            .setMaxValues(showsInChannel.length)
            .addOptions(showsInChannel.map(s => {
              return {
                label: s.name,
                value: s.imdbId
              }
            }))
        )

      await interaction.editReply({ content: `Select the shows that you'd like to unlink from <#${channel?.id}>`, components: [row] })

    } catch (error) {
      // catch our custom error and display it for the user
      if (error instanceof ProgressError) {
        const message = `${progress.toString()}\n\nError: ${error.message}`
        return await interaction.editReply(message)
      }

      throw error
    }
  }
}
