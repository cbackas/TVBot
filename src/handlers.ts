import { ChannelType, ClientEvents } from "npm:discord.js"
import { Settings } from "lib/settingsManager.ts"
import { pruneUnsubscribedShows, removeAllSubscriptions } from "lib/shows.ts"

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
    await Settings.removeGlobalDestination(channel.id)
  }
}
