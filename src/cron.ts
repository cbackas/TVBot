import {
  setRandomShowActivity,
  setTVDBLoadingActivity,
} from "lib/discordActivities.ts"
import { logMessage, reportError } from "lib/discordLog.ts"
import { getEnv } from "lib/env.ts"
import { sendAiringMessages } from "lib/episodeNotifier.ts"
import { sendMorningSummary } from "lib/morningSummary.ts"
import { Settings } from "lib/settingsManager.ts"
import { checkForAiringEpisodes, pruneUnsubscribedShows } from "lib/shows.ts"

async function runJob(
  name: string,
  job: () => Promise<void> | void,
): Promise<void> {
  try {
    await job()
  } catch (error) {
    reportError(`Cron: ${name}`, error, { type: "task_errors" })
  }
}

export function scheduleCronJobs() {
  Deno.cron("Announcements", { minute: { every: 10, start: 8 } }, () => {
    void runJob("Announcements", sendAiringMessages)
    void runJob("Random activity", setRandomShowActivity)
  })

  Deno.cron("Fetch Episode Data", { hour: { every: 4 } }, async () => {
    await runJob("Fetch Episode Data", async () => {
      setTVDBLoadingActivity()
      await pruneUnsubscribedShows()
      const { checked, failed, upcomingEpisodes } = await checkForAiringEpisodes()
      const shows = `${checked} ${checked === 1 ? "show" : "shows"}`
      const episodes = `${upcomingEpisodes} upcoming ${
        upcomingEpisodes === 1 ? "episode" : "episodes"
      }`
      const failures = failed > 0 ? `, ${failed} failed` : ""
      logMessage(
        `Fetch Episode Data: checked ${shows}, ${episodes}${failures}`,
        "tasks",
      )
    })
  })

  Deno.cron("Morning Summary", { hour: 8, minute: 0 }, async () => {
    await runJob("Morning Summary", async () => {
      const settings = Settings.fetch()
      if (settings == null) throw new Error("Settings not found")
      await sendMorningSummary()
    })
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
