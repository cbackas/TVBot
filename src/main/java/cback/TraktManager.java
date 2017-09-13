package cback;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.*;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.IdType;
import com.uwetrottmann.trakt5.enums.Type;
import retrofit2.Response;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TraktManager {

    private TraktV2 trakt;
    private TVBot bot;

    public TraktManager(TVBot bot) {
        this.bot = bot;

        Optional<String> traktToken = bot.getConfigManager().getTokenValue("traktToken");
        if (!traktToken.isPresent()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your Trakt token in the config.");
            System.out.println("Exiting......");
            System.out.println("-------------------------------------");
            System.exit(0);
            return;
        }
        trakt = new TraktV2(traktToken.get());

    }

    public void updateAiringData() {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            Response<List<CalendarShowEntry>> response = trakt.calendars().shows(dateFormat.format(date), 3).execute();
            if (response.isSuccessful()) {
                List<CalendarShowEntry> shows = response.body();
                List<String> desiredShows = bot.getDatabaseManager().getTV().getShowIDs();
                for (CalendarShowEntry entry : shows) {
                    String id = entry.show.ids.imdb;
                    if (desiredShows.contains(id)) {
                        //int airTime = Util.toInt(entry.first_aired.toMillis() / 1000);
                        int airTime = Math.toIntExact(entry.first_aired.toInstant().toEpochMilli() / 1000);
                        int currentTime = Util.getCurrentTime();
                        String episodeID = String.valueOf(entry.episode.ids.trakt);
                        //don't add if already aired or if airing already in database
                        if ((bot.getDatabaseManager().getTV().getAiring(episodeID) == null) && (airTime > currentTime)) {
                            String episodeInfo = "S" + entry.episode.season + "E" + entry.episode.number + " - " + entry.episode.title;
                            bot.getDatabaseManager().getTV().insertAiring(episodeID, id, airTime, episodeInfo, "NONE");
                            System.out.println("Found Show Airing: " + entry.show.title + ": " + episodeInfo + " - " + airTime);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Util.reportHome(e);
        }
    }

    public String getShowTitle(String imdbID) {
        try {
            //Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, imdbID, 1, 1).execute();
            Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, imdbID, Type.SHOW, Extended.NOSEASONS, 1, 1).execute();
            if (search.isSuccessful() && !search.body().isEmpty()) {
                return search.body().get(0).show.title;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Show showSummary(String imdbID) {
        try {
            Response<Show> show = trakt.shows().summary(imdbID, Extended.FULL).execute();
            if (show.isSuccessful()) {
                return show.body();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Show showSummaryFromName(String showName) {
        try {
            //Response<List<SearchResult>> search = trakt.search().textQuery(showName, Type.SHOW, null, 1, 1).execute();
            Response<List<SearchResult>> search = trakt.search().textQuery(Type.SHOW, showName, null, null, null, null, null, null, Extended.FULL, 1, 1).execute();
            if (search.isSuccessful() && !search.body().isEmpty()) {
                Response<Show> show = trakt.shows().summary(search.body().get(0).show.ids.imdb, Extended.FULL).execute();
                if (show.isSuccessful()) {
                    return show.body();
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Movie movieSummaryFromName(String movieName) {
        try {
            Response<List<SearchResult>> search = trakt.search().textQuery(Type.MOVIE, movieName, null, null, null, null, null, null, Extended.FULL, 1, 1).execute();
            if (search.isSuccessful() && !search.body().isEmpty()) {
                Response<Movie> movie = trakt.movies().summary(search.body().get(0).movie.ids.imdb, Extended.FULL).execute();
                if (movie.isSuccessful()) {
                    return movie.body();
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

}