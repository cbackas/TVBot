import { CacheType, Channel, ChannelType, ChatInputCommandInteraction, ForumChannel, PermissionFlagsBits, SlashCommandBuilder } from 'discord.js'
import client, { DBChannelType } from '../lib/prisma'
import { Command } from '../interfaces/command'
import { Prisma } from '@prisma/client'
import { ProgressMessageBuilder } from '../lib/progressMessages'
import { App } from '../app'
import { getSeriesByImdbId } from '../lib/tvdb'
import { updateDBEpisodes } from '../lib/dbManager'
import { scheduleAiringMessages } from '../lib/episodeNotifier'

const slashCommand = new SlashCommandBuilder()
  .setName('post')
  .setDescription('Create a forum post for a show. Require "Manage Channels" permission.')
  .setDMPermission(false)
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageChannels)
  .addStringOption(option => option.setName('imdb_id')
    .setDescription('The IMDB ID to search for')
    .setMinLength(9)
    .setRequired(true)
  )

const command: Command = {
  data: slashCommand,
  async execute(app: App, interaction: ChatInputCommandInteraction<CacheType>) {
    const imdbId = interaction.options.getString('imdb_id', true)

    switch (interaction.options.getSubcommand()) {
      case 'add':
        return await addShow(app, interaction, imdbId)
      default:
        return await interaction.editReply('Invalid subcommand')
    }
  }
}

export default command

const addShow = async (app: App, interaction: ChatInputCommandInteraction<CacheType>, imdbId: string) => {
  const progressMessage = new ProgressMessageBuilder()
    .addStep(`Checking for existing forum posts with ID \`${imdbId}\``)
    .addStep(`Fetching show data`)
    .addStep('Creating forum post')
    .addStep('Saving show to DB')
    .addStep('Fetching upcoming episodes')

  // start step 1
  await interaction.followUp(progressMessage.nextStep())

  const settings = app.getSettings()
  const tvForum = settings.find(s => s.key === 'tv_forum')?.value

  if (!tvForum) {
    return await interaction.editReply(progressMessage.toString() + '\n\nError: No TV forum configured, use /settings tv_forum <channel> to set the default TV forum')
  }

  const existingShowDestinations = await client.showDestination.findMany({
    where: {
      channelType: DBChannelType.FORUM,
      channelId: tvForum,
      show: {
        imdbId: imdbId
      },
    },
    select: {
      showId: true,
      channelId: true,
      show: {
        select: {
          id: true,
          name: true,
          imdbId: true,
          tvdbId: true
        }
      }
    }
  })

  if (existingShowDestinations.length > 0) {
    const channel = await interaction.client.channels.fetch(existingShowDestinations[0].channelId)

    if (channel == null || !isForumChannel(channel)) {
      return await interaction.editReply(`${progressMessage.toString()}\n\nError: A forum post already exists for that show, but the channel could not be found. cback should fix this.`)
    }

    return await interaction.editReply(`${progressMessage.toString()}\n\nA forum post already exists for that show <#${channel.id}>`)
  }

  // start step 2
  await interaction.followUp(progressMessage.nextStep())

  const tvdbSeries = await getSeriesByImdbId(imdbId)

  if (!tvdbSeries) {
    return await interaction.editReply(`${progressMessage.toString()}\n\nNo show found with that IMDB ID`)
  }

  // start step 3
  await interaction.editReply(progressMessage.nextStep())

  const forumChannel = await interaction.client.channels.fetch(tvForum)
  if (forumChannel == null || !isForumChannel(forumChannel)) {
    return await interaction.editReply(`${progressMessage.toString()}\n\nError: No tv forum found`)
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
          channelType: DBChannelType.FORUM,
        }
      }
    })

    const newShow = await client.show.create({
      data
    })

    // start step 5
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

  // finish step 5
  console.log(`Added show ${tvdbSeries.name} (${imdbId})`)
  return await interaction.editReply(progressMessage.nextStep() + `\n\nCreated post [${tvdbSeries.name}](${newPost.url})`)
}

const isForumChannel = (channel: Channel): channel is ForumChannel => {
  return channel.type == ChannelType.GuildForum
}
