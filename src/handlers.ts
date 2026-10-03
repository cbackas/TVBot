import { ChannelType, ClientEvents } from "discord.js"
import { escapeLogText, logMessage, reportError } from "lib/discordLog.ts"
import client from "lib/prisma.ts"
import { Settings } from "lib/settingsManager.ts"
import { pruneUnsubscribedShows, removeAllSubscriptions } from "lib/shows.ts"

/**
 * Check if a thread has at least one show linked to it
 * @param threadId id of the thread to check
 */
async function isTrackedThread(threadId: string): Promise<boolean> {
  const count = await client.show.count({
    where: { destinations: { some: { channelId: threadId } } },
  })
  return count > 0
}

/**
 * When a thread (forum post) is deleted, remove all subscriptions for that post
 */
export async function handleThreadDelete(
  ...args: ClientEvents["threadDelete"]
) {
  const [thread] = args

  // check before the subscriptions are removed below
  try {
    if (await isTrackedThread(thread.id)) {
      logMessage(
        `Show thread deleted: **${escapeLogText(thread.name)}** (<#${thread.parentId}>)`,
        "thread_ops",
      )
    }
  } catch (error) {
    reportError(`Checking thread ${thread.id}`, error)
  }

  await removeAllSubscriptions(thread.id, "channelId")
  await pruneUnsubscribedShows()
}

/**
 * Log when a tracked show thread is renamed or locked/unlocked
 */
export async function handleThreadUpdate(
  ...args: ClientEvents["threadUpdate"]
): Promise<void> {
  const [oldThread, newThread] = args
  const renamed = oldThread.name !== newThread.name
  const lockChanged = oldThread.locked !== newThread.locked
  if (!renamed && !lockChanged) return

  try {
    if (!(await isTrackedThread(newThread.id))) return
  } catch (error) {
    reportError(`Checking thread ${newThread.id}`, error)
    return
  }

  if (renamed) {
    logMessage(
      `Show thread renamed: **${escapeLogText(oldThread.name)}** → [**${
        escapeLogText(newThread.name)
      }**](${newThread.url})`,
      "thread_ops",
    )
  }

  if (lockChanged) {
    const action = newThread.locked
      ? "Show thread locked"
      : "Show thread unlocked"
    logMessage(`${action}: <#${newThread.id}>`, "thread_ops")
  }
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

    // stop trying to post logs to a channel that no longer exists
    if (Settings.fetch()?.logChannel === channel.id) {
      await Settings.update({ logChannel: null })
    }
  }
}
