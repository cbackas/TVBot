const SONARR_URL = Deno.env.get("SONARR_URL") || "http://localhost:8989"
const API_KEY = Deno.env.get("SONARR_API_KEY")

interface SonarrCalendarItem {
    seriesId: number
    title: string
    seasonNumber: number
    episodeNumber: number
    airDateUtc: string
    series: {
        title: string
        imdbId: string
        tvdbId: number
    }
}

async function getUpcomingShows() {
    if (!API_KEY) throw new Error("SONARR_API_KEY environment variable not set")

    const now = new Date()
    const nextWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)

    const url = new URL(`${SONARR_URL}/api/v3/calendar`)
    url.searchParams.set("start", now.toISOString())
    url.searchParams.set("end", nextWeek.toISOString())
    url.searchParams.set("includeSeries", "true")

    const response = await fetch(url.toString(), {
        headers: { "X-Api-Key": API_KEY },
    })

    if (!response.ok) {
        throw new Error(
            `Sonarr API error: ${response.status} - ${await response.text()}`,
        )
    }

    const data: SonarrCalendarItem[] = await response.json()

    return data.map((item) => ({
        title: item.series.title,
        imdbId: item.series.imdbId,
        airDate: item.airDateUtc,
        season: item.seasonNumber,
        episode: item.episodeNumber,
    }))
}

const shows = (await getUpcomingShows()).map((show) => show.imdbId)
for (let i = 0; i < shows.length; i += 10) {
    const chunk = shows.slice(i, i + 10)
    console.log(chunk.join(","))
}
