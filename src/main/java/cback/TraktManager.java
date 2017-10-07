package cback;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;
import com.uwetrottmann.tmdb2.services.SearchService;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.*;
import com.uwetrottmann.trakt5.enums.Extended;
import retrofit2.Response;

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

    public Show showSummaryFromName(String showName) {
        try {
            Response<Show> show = trakt.shows().summary(searchTmdbShow(showName), Extended.FULL).execute();
            if (show.isSuccessful()) {
                return show.body();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Movie movieSummaryFromName(String movieName) {
        try {
            Response<Movie> movie = trakt.movies().summary(searchTmdbMovie(movieName), Extended.FULL).execute();
            if (movie.isSuccessful()) {
                return movie.body();
            }
        } catch (Exception e) {
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
                    System.out.println(show.body().imdb_id);
                    return show.body().imdb_id;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

}