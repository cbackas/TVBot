import { CacheType, Channel, ChannelType, ChatInputCommandInteraction, ForumChannel, GuildForumThreadManager, PermissionFlagsBits, SlashCommandBuilder, SlashCommandStringOption, SlashCommandSubcommandBuilder } from 'discord.js'
import client from '../lib/prisma'
import { Command } from '../interfaces/command'
import { Prisma } from '@prisma/client'
import { findByExternalId, getAiringSoon, getExternalIds } from '../lib/dataFetching'
import { ProgressMessageBuilder, StepStatus } from '../lib/progressMessages'
import { App } from '../app'

const imdbOption = (option: SlashCommandStringOption) => {
  return option.setName('imdb_id')
    .setDescription('The IMDB ID to search for')
    .setMinLength(9)
    .setRequired(true)
}

const slashCommand = new SlashCommandBuilder()
  .setName('show')
  .setDescription('Create a forum post for a show. Required "Manage Channels" permission.')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
  .addSubcommand(new SlashCommandSubcommandBuilder()
    .setName('add')
    .setDescription('Create a new show post in the forum')
    .addStringOption(option => imdbOption(option))
  )
  .addSubcommand(new SlashCommandSubcommandBuilder()
    .setName('remove')
    .setDescription('Remove a post from the forum and removes it from the database')
    .addStringOption(option => imdbOption(option))
  )

const command: Command = {
  data: slashCommand,
  async execute(app: App, interaction: ChatInputCommandInteraction<CacheType>) {
    const imdbId = interaction.options.getString('imdb_id', true)

    switch (interaction.options.getSubcommand()) {
      case 'add':
        return await addShow(app, interaction, imdbId)
      case 'remove':
        return await removeShow(app, interaction, imdbId)
      default:
        return await interaction.editReply('Invalid subcommand')
    }
  }
}

export default command

const addShow = async (app: App, interaction: ChatInputCommandInteraction<CacheType>, imdbId: string) => {
  const progressMessage = new ProgressMessageBuilder()
    .addStep(`Searching for show with IMDB ID ${imdbId}`)
    .addStep('Fetching related data')
    .addStep('Creating forum post')
    .addStep('Saving to database')

  // start step 1
  await interaction.followUp(progressMessage.nextStep())

  const find = await findByExternalId(imdbId, 'imdb_id')

  if (!find.tv_results || find.tv_results.length !== 1) {
    return await interaction.editReply(progressMessage.toString() + '\n\nNo show found with that imdb id')
  }

  const show = find.tv_results[0]

  // start step 2
  await interaction.editReply(progressMessage.nextStep())

  const externalIds = await getExternalIds(show.id)

  if (!externalIds) {
    return await interaction.editReply(progressMessage.toString() + '\n\nNo external ids found for this show')
  }

  const tvdbId = externalIds.tvdb_id

  if (!tvdbId) {
    return await interaction.editReply(progressMessage.toString() + '\n\nError: No tvdb id found for this show')
  }

  // start step 3
  await interaction.editReply(progressMessage.nextStep())

  const settings = app.getSettings()

  const tvForum = settings.find(s => s.key === 'tv_forum')?.value

  if (!tvForum) {
    return await interaction.editReply(progressMessage.toString() + '\n\nError: No tv forum found')
  }


  // const manager = new GuildForumThreadManager()

  const forumChannel = await interaction.client.channels.fetch(tvForum)
  if (forumChannel == null || !isForumChannel(forumChannel)) {
    return await interaction.editReply(progressMessage.toString() + '\n\nError: No tv forum found')
  }

  const newPost = await forumChannel.threads.create({
    name: show.name,
    autoArchiveDuration: 1440,
    message: {
      content: `https://image.tmdb.org/t/p/w300_and_h450_bestv2${show.poster_path}`
    }
  })

  await interaction.editReply(progressMessage.nextStep())

  try {
    const data = Prisma.validator<Prisma.ShowCreateInput>()({
      name: show.name,
      imdbId: imdbId,
      tmdbId: show.id,
      tvdbId: tvdbId,
      ShowPost: {
        create: [
          {
            forumPost: {
              create: {
                forumId: tvForum,
                postId: newPost.id
              }
            }
          }
        ]
      }
    })

    await client.show.create({
      data
    })
  } catch (error) {
    if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
      return await interaction.editReply(progressMessage.toString() + '\n\nError: Show already exists')
    }
    console.log(error)
    return await interaction.editReply(progressMessage.toString() + '\n\nError: Something went wrong')
  }

  return await interaction.editReply(progressMessage.nextStep() + `\n\nCreated thread <#${newPost.id}>`)
}

const isForumChannel = (channel: Channel): channel is ForumChannel => {
  return channel.type == ChannelType.GuildForum
}

const removeShow = async (app: App, interaction: ChatInputCommandInteraction<CacheType>, imdbId: string) => {
  return await interaction.editReply(`Adding show with imdb id ${imdbId}`)
}
