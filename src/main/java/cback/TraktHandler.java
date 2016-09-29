package cback;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.CalendarShowEntry;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.enums.IdType;
import retrofit2.Response;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TraktHandler {

    private TraktV2 trakt;
    private TVbot bot;

    public TraktHandler(TVbot bot) {
        this.bot = bot;
        trakt = new TraktV2("56c1b90ae2c41ee598ff2d2606ff2fefb6f59516c9f11aedd165020a06b2b6fd");
    }

    public void updateAiringData() {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            Response<List<CalendarShowEntry>> response = trakt.calendars().shows(dateFormat.format(date), 1).execute();
            if (response.isSuccessful()) {
                List<CalendarShowEntry> shows = response.body();
                List<String> desiredShows = bot.getDatabaseManager().getShowIDs();
                for (CalendarShowEntry entry : shows) {
                    String id = entry.show.ids.imdb;
                    if (desiredShows.contains(id)) {

                        long time = entry.first_aired.getMillis() / 1000;
                        String episodeID = entry.episode.ids.imdb;
                        String episode = "S" + entry.episode.season + "E" + entry.episode.number;
                        String episodeTitle = entry.episode.title;
                        bot.getDatabaseManager().insertAiring(episodeID, id, time, episode, episodeTitle);
                        System.out.println("Added Show Airing: " + entry.show.title + " - " + episode + " - " + time);
                    }
                }
            }
        } catch (IOException e) {
            // could not connect to trakt
        }
    }

    public String getShowTitle(String imdbID) {
        try {
            Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, imdbID, 1, 1).execute();
            if (search.isSuccessful() && search.body().isEmpty()) {
                return search.body().get(0).show.title;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}