import { ChannelType, ClientEvents } from "npm:discord.js"
import { pruneUnsubscribedShows, removeAllSubscriptions } from "lib/shows.ts"
import { getSetting, setSetting, type Settings } from "database/settings.ts"

/**
 * When a thread (forum post) is deleted, remove all subscriptions for that post
 */
export async function handleThreadDelete(
  ...args: ClientEvents["threadDelete"]
) {
  const [thread] = args
  await removeAllSubscriptions(thread.id, "channelId")
  await pruneUnsubscribedShows()
}

/**
 * When a forum is deleted, remove all subscriptions for post in that forum
 */
export async function handleChannelDelete(
  ...args: ClientEvents["channelDelete"]
) {
  const [channel] = args
  if (channel.type === ChannelType.GuildForum) {
    await removeAllSubscriptions(channel.id, "forumId")
    await pruneUnsubscribedShows()
  }

  if (channel.type === ChannelType.GuildText) {
    await removeAllSubscriptions(channel.id, "channelId")
    await pruneUnsubscribedShows()

    const destinations: Settings["allEpisodes"] = await getSetting(
      "allEpisodes",
    )
    const filteredDestinations: Settings["allEpisodes"] = destinations.filter((
      d: Settings["allEpisodes"][number],
    ) => d.channelId !== channel.id)
    if (filteredDestinations.length !== destinations.length) {
      await setSetting("allEpisodes", filteredDestinations)
    }
  }
}
