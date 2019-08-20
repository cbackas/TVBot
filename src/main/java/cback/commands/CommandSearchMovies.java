package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import com.uwetrottmann.trakt5.entities.Movie;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        String movieName = String.join(" ", args);

        Message initialMessage = Util.simpleEmbed(commandEvent.getTextChannel(), "Searching <https://trakt.tv/> for " + movieName + " ...");

        Movie movieData = bot.getTraktManager().movieSummaryFromName(movieName);
        if(movieData != null) {
            String title = "**" + movieData.title + " (" + Integer.toString(movieData.year) + ")**";
            String overview = movieData.overview;
            String premier = movieData.released.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            String rating = movieData.certification;
            String runtime = Integer.toString(movieData.runtime);
            String country = movieData.language;
            String homepage = "<https://trakt.tv/movies/" + movieData.ids.slug + ">\n<http://www.imdb.com/title/" + movieData.ids.imdb + ">";

            EmbedBuilder embed = Util.getEmbed(commandEvent.getAuthor());

            if(movieData.tagline != null) {
                embed.setTitle(movieData.tagline);
            }

            embed.setTitle(title);
            embed.setDescription(overview);
            embed.addField("Referenced:", homepage, false);
            embed.addField("PREMIERED:", premier, true);
            embed.addField("RUNTIME:", runtime, true);
            embed.addField("RATED:", rating, true);
            embed.addField("LANGUAGE:", country.toUpperCase(), true);
            embed.addField("GENRES:", String.join(", ", movieData.genres), true);

            initialMessage.editMessage("Found it!");
            embed.setColor(Util.getBotColor()).build();
        } else {
            initialMessage.editMessage("Oops...");
            new EmbedBuilder().setDescription("Error: Movie not found").setColor(Util.getBotColor()).build();
            Util.simpleEmbed(commandEvent.getGuild().getTextChannelById(Channels.ERRORLOG_CH_ID.getId()), "Couldn't find movie " + movieName + " in " + commandEvent.getGuild().getName() + "/" + commandEvent.getChannel().getName());
        }
    }
}