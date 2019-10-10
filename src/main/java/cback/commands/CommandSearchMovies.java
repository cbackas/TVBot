package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.uwetrottmann.tmdb2.entities.Movie;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CommandSearchMovies extends Command {

    private TVBot bot;

    public CommandSearchMovies() {
        this.bot = TVBot.getInstance();
        this.name = "movie";
        this.arguments = "move [movie name]";
        this.help = "Searches trakt.tv for the provided movie. Sometimes it works, sometimes it don't";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {

//        Util.simpleEmbedSync(commandEvent.getTextChannel(), "This command is under maintenance..");

//        String movieArg = commandEvent.getArgs();
//        Movie result = bot.getTraktManager().movieSummaryFromName(movieArg);
//
//        return;


        String movieArg = commandEvent.getArgs();

        Message initialMessage = Util.simpleEmbedSync(commandEvent.getTextChannel(), "Searching <https://trakt.tv/> for " + movieArg + " ...");

        Movie result = bot.getTraktManager().movieSummaryFromName(movieArg);

        if (result != null) {

            Calendar releaseDate = Calendar.getInstance();
            releaseDate.setTime(result.release_date);

            String title = "**" + result.title + " (" + releaseDate.get(Calendar.YEAR) + ")**";
            String overview = result.overview;
            String premier = new SimpleDateFormat("MMM dd, yyyy").format(result.release_date);
            //String rating = result.release_dates //TODO fix
            String runtime = result.runtime + " min";
            String country = result.original_language;
            String homepage = "<https://www.themoviedb.org/movie/" + result.id + ">\n<http://www.imdb.com/title/" + result.imdb_id + ">";

            EmbedBuilder embed = Util.getEmbed(commandEvent.getAuthor());

            //TODO whats this
//            if(result.tagline != null) {
//                embed.setTitle(movieData.tagline);
//            }

            embed.setTitle(title);
            embed.setDescription(overview);
            embed.addField("Referenced:", homepage, false);
            embed.addField("PREMIERED:", premier, true);
            embed.addField("RUNTIME:", runtime, true);
//            embed.addField("RATED:", rating, true); //TODO fix
            embed.addField("LANGUAGE:", country.toUpperCase(), true);
            embed.addField("GENRES:", StringUtils.join(result.genres.stream().map(genre -> genre.name).toArray(), ","), true);

            embed.setColor(Util.getBotColor());

            initialMessage.editMessage("Found it!").queue();
            initialMessage.editMessage(embed.build()).queue();

        } else {

            var errorEmbed = new EmbedBuilder().setDescription("Error: Movie not found").setColor(Util.getBotColor()).build();
            initialMessage.editMessage("Oops...").queue();
            initialMessage.editMessage(errorEmbed).queue();
        }
    }
}