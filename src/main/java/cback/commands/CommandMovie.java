package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Movie;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandMovie implements Command {
    @Override
    public String getName() {
        return "movie";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String movieName = Arrays.stream(args).collect(Collectors.joining(" "));
        Movie movieData = bot.getTraktManager().movieSummaryFromName(movieName);
        if (movieData != null) {
            String title = "**" + movieData.title + " (" + Integer.toString(movieData.year) + ")**";
            String overview = movieData.overview;
            String premier = new SimpleDateFormat("MMM dd, yyyy").format(movieData.released.toDate());
            String rating = movieData.certification;
            String runtime = Integer.toString(movieData.runtime);
            String country = movieData.language;
            String homepage = "<https://trakt.tv/movies/" + movieData.ids.slug + ">";
            if (movieData.tagline != null) {
                title = movieData.tagline + "\n\n**" + movieData.title + " (" + Integer.toString(movieData.year) + ")**";
            }
            Util.sendMessage(message.getChannel(),
                    title + "\n" +
                            overview + "\n" +
                            homepage + "\n" +
                            "```\n" +
                            "PREMIERED: " + premier + "\n" +
                            "RUNTIME: " + runtime + " min\n" +
                            "RATED: " + rating + "\n" +
                            "Language: " + country.toUpperCase() + "\n" +
                            "GENRES: " + String.join(", ", movieData.genres) + "\n" +
                            "```\n");
        } else {
            Util.sendMessage(message.getChannel(), "Error: Movie not found");
        }
    }

}
