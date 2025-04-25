import { ActivityType } from "npm:discord.js"
import client from "lib/prisma.ts"
import { getClientUser } from "app.ts"

/**
 * sets the bots activity to a random show from the bot db
 * @param clientUser the discord user to set the activity for
 */
export async function setRandomShowActivity(): Promise<void> {
  const showCount = await client.show.count()
  const randomIndex = Math.floor(Math.random() * showCount)
  const show = await client.show.findMany({
    skip: randomIndex,
    take: 1,
  })

  if (show.length !== 1) {
    clearActivity()
    return
  }

  setWatchingActivity(show[0].name)
}

/**
 * sets watching activity for the bot for a show
 * @param clientUser the discord user to set the activity for
 * @param show show name to put in the activity
 */
export function setWatchingActivity(
  show: string,
): void {
  const clientUser = getClientUser()
  clientUser.setActivity(show, { type: ActivityType.Watching })
}

/**
 * this clears the bots activity
 * @param clientUser the discord user to set the activity for
 */
export function clearActivity(): void {
  const clientUser = getClientUser()
  console.info("Clearing activity")
  clientUser.setActivity()
}

/**
 * this sets a dumb 'loading' activity for while the bot is fetching stuff from the TVDB
 * @param clientUser the discord user to set the activity for
 */
export function setTVDBLoadingActivity(): void {
  const clientUser = getClientUser()
  clientUser.setActivity("episode data from the TVDB", {
    type: ActivityType.Playing,
  })
}
