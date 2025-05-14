import {
  setRandomShowActivity,
  setTVDBLoadingActivity,
} from "lib/discordActivities.ts"
import { getEnv } from "lib/env.ts"
import { sendAiringMessages } from "lib/episodeNotifier.ts"
import { sendMorningSummary } from "lib/morningSummary.ts"
import { checkForAiringEpisodes, pruneUnsubscribedShows } from "lib/shows.ts"

export function scheduleCronJobs() {
  Deno.cron("Announcements", { minute: { every: 10, start: 8 } }, () => {
    void sendAiringMessages()
    void setRandomShowActivity()
  })

  Deno.cron("Fetch Episode Data", { hour: { every: 4 } }, async () => {
    setTVDBLoadingActivity()
    await pruneUnsubscribedShows()
    await checkForAiringEpisodes()
  })

  Deno.cron("Morning Summary", { hour: 8, minute: 0 }, async () => {
    await sendMorningSummary()
  })

  const healthcheckUrl = getEnv("HEALTHCHECK_URL")
  if (healthcheckUrl != null) {
    Deno.cron("Healthcheck", { minute: { every: 1 } }, async () => {
      try {
        await fetch(healthcheckUrl)
        console.debug("[Healthcheck] Healthcheck ping sent")
      } catch (error) {
        console.error("[Healthcheck] Healthcheck ping failed", error)
      }
    })
  }
}
