import { CacheType, Channel, ChannelType, ChatInputCommandInteraction, ForumChannel, PermissionFlagsBits, SlashCommandBuilder, SlashCommandStringOption, SlashCommandSubcommandBuilder } from 'discord.js'
import client from '../lib/prisma'
import { Command } from '../interfaces/command'
import { Prisma } from '@prisma/client'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { updateDBEpisodes } from '../lib/dbManager'
import { scheduleAiringMessages } from '../lib/episodeNotifier'

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
    .addStep('Creating forum post')
    .addStep('Saving to database')
    .addStep('Save upcoming episodes to database')

  // start step 1
  await interaction.followUp(progressMessage.nextStep())

  // const find = await findByExternalId(imdbId, 'imdb_id')

  const tvdbSeries = await getSeriesByImdbId(imdbId)

  if (!tvdbSeries) {
    return await interaction.editReply(progressMessage.toString() + '\n\nNo show found with that imdb id')
  }

  // start step 2
  await interaction.editReply(progressMessage.nextStep())

  const settings = app.getSettings()
  const tvForum = settings.find(s => s.key === 'tv_forum')?.value

  if (!tvForum) {
    return await interaction.editReply(progressMessage.toString() + '\n\nError: No tv forum configured, use /settings tv_forum <channel> to set one')
  }

  const forumChannel = await interaction.client.channels.fetch(tvForum)
  if (forumChannel == null || !isForumChannel(forumChannel)) {
    return await interaction.editReply(progressMessage.toString() + '\n\nError: No tv forum found')
  }

  const newPost = await forumChannel.threads.create({
    name: tvdbSeries.name,
    autoArchiveDuration: 10080,
    message: {
      content: `${tvdbSeries.image}`
    }
  })

  // start step 4
  await interaction.editReply(progressMessage.nextStep())

  try {
    const data = Prisma.validator<Prisma.ShowCreateInput>()({
      name: tvdbSeries.name,
      imdbId: imdbId,
      tvdbId: tvdbSeries.id,
      ShowDestination: {
        create: {
          channelId: newPost.id,
          forumId: tvForum,
          channelType: 'FORUM',
        }
      }
    })

    const newShow = await client.show.create({
      data
    })

    await interaction.editReply(progressMessage.nextStep())

    await updateDBEpisodes(newShow)
    await scheduleAiringMessages(app)
  } catch (error) {
    if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
      return await interaction.editReply(progressMessage.toString() + '\n\nError: Show already exists')
    }
    console.log(error)
    return await interaction.editReply(progressMessage.toString() + '\n\nError: Something went wrong')
  }

  // finish step 4
  console.log(`Added show ${tvdbSeries.name} (${imdbId})`)
  return await interaction.editReply(progressMessage.nextStep() + `\n\nCreated post [${tvdbSeries.name}](${newPost.url})`)
}

const isForumChannel = (channel: Channel): channel is ForumChannel => {
  return channel.type == ChannelType.GuildForum
}

const removeShow = async (app: App, interaction: ChatInputCommandInteraction<CacheType>, imdbId: string) => {
  return await interaction.editReply(`Adding show with imdb id ${imdbId}`)
}
