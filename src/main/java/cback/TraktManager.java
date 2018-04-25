package cback;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;
import com.uwetrottmann.tmdb2.services.SearchService;
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

public class TraktManager {

    private TraktV2 trakt;
    private Tmdb tmdb;
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

        Optional<String> tmdbToken = bot.getConfigManager().getTokenValue("tmdbToken");
        if (!tmdbToken.isPresent()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your tmdb token in the config.");
            System.out.println("Exiting......");
            System.out.println("-------------------------------------");
            System.exit(0);
            return;
        }
        tmdb = new Tmdb(tmdbToken.get());
    }

    public void updateAiringData() {
        try {
            System.out.println("Grabbing new airings from Trakt...");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            Response<List<CalendarShowEntry>> response = trakt.calendars().shows(dateFormat.format(date), 7).execute();
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
                            bot.getDatabaseManager().getTV().insertAiring(episodeID, id, airTime, episodeInfo, false);
                            System.out.println("Found Show Airing: " + entry.show.title + ": " + episodeInfo + " - " + airTime);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Util.reportHome(e);
        }
    }

    public String getShowTitle(String imdbID) {
        try {
            Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, imdbID, Type.SHOW, Extended.NOSEASONS, 1, 1).execute();
            if (search.isSuccessful() && !search.body().isEmpty()) {
                return search.body().get(0).show.title;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Show showSummary(String imdbID) {
        try {
            Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, imdbID, Type.SHOW, Extended.FULL, 1, 1).execute();
            if (search.isSuccessful()) {
                return search.body().get(0).show;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Show showSummaryFromName(String showName) {
        try {
            Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, searchTmdbShow(showName), Type.SHOW, Extended.FULL, 1, 1).execute();
            if (search.isSuccessful()) {
                return search.body().get(0).show;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Movie movieSummaryFromName(String movieName) {
        try {
            Response<List<SearchResult>> search = trakt.search().idLookup(IdType.IMDB, searchTmdbMovie(movieName), Type.MOVIE, Extended.FULL, 1, 1).execute();
            if (search.isSuccessful()) {
                return search.body().get(0).movie;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String searchTmdbMovie(String movieName) {
        try {
            SearchService service = tmdb.searchService();
            Response<MovieResultsPage> search = service.movie(movieName, 1, null, true, null, null, null).execute();
            if (search.isSuccessful() && !search.body().results.isEmpty()) {
                Response<com.uwetrottmann.tmdb2.entities.Movie> movie = tmdb.moviesService().summary(search.body().results.get(0).id).execute();
                if (movie.isSuccessful()) {
                    return movie.body().imdb_id;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String searchTmdbShow(String showName) {
        try {
            SearchService service = tmdb.searchService();
            Response<TvShowResultsPage> search = service.tv(showName, 1, null, null, null).execute();
            if (search.isSuccessful() && !search.body().results.isEmpty()) {
                Response<com.uwetrottmann.tmdb2.entities.TvExternalIds> show = tmdb.tvService().externalIds(search.body().results.get(0).id, null).execute();
                if (show.isSuccessful()) {
                    return show.body().imdb_id;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}