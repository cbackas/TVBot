package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommandSearchShows extends Command {

    private TVBot bot;

    public CommandSearchShows() {
        this.bot = TVBot.getInstance();
        this.name = "show";
        this.arguments = "show [show name]";
        this.help = "Searches trakt.tv for the provided show. Sometimes it works, sometimes it don't";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        String showName = String.join(" ", args);
        Message initialMessage = Util.simpleEmbed(commandEvent.getTextChannel(), "Searching <https://trakt.tv/> for " + showName + " ...");

        Show showData = bot.getTraktManager().showSummaryFromName(showName);
        if(showData != null) {
            String title = showData.title + " (" + Integer.toString(showData.year) + ") ";
            String overview = showData.overview;
            String airs = (showData.status == Status.RETURNING || showData.status == Status.IN_PRODUCTION)
                    ? showData.airs.day + " at " + Util.to12Hour(showData.airs.time) + " EST on " + showData.network : "Ended";
            String premier = new SimpleDateFormat("MMM dd, yyyy").format(new Date(showData.first_aired.toInstant().toEpochMilli()));
            String runtime = Integer.toString(showData.runtime);
            String country = showData.country + " - " + showData.language;
            String homepage = "<https://trakt.tv/shows/" + showData.ids.slug + ">\n<http://www.imdb.com/title/" + showData.ids.imdb + ">";

            try {
                overview = commandEvent.getGuild().getTextChannelById(Long.parseLong(bot.getDatabaseManager().getTV().getShow(showData.ids.imdb).getChannelID())).getAsMention() + "\n" + overview;
            } catch(Exception ignore) {
            }

            EmbedBuilder embed = Util.getEmbed(commandEvent.getAuthor());

            embed.setTitle(title);
            embed.setDescription(overview);
            embed.addField("References:", homepage, false);
            embed.addField("AIRS:", airs, true);
            embed.addField("RUNTIME:" , runtime, true);
            embed.addField("PREMIERED:", premier, true);
            embed.addField("COUNTRY:", country.toUpperCase(), true);
            embed.addField("GENRES:", String.join(", ", showData.genres), true);

            initialMessage.editMessage("Found it!");
            embed.setColor(Util.getBotColor()).build();
        } else {
            initialMessage.editMessage("Oops...");
            new EmbedBuilder().setDescription("Error: Show not found").setColor(Util.getBotColor()).build();
            Util.simpleEmbed(commandEvent.getGuild().getTextChannelById(Channels.ERRORLOG_CH_ID.getId()), "Couldn't find show " + showName + " in " + commandEvent.getGuild().getName() + "/" + commandEvent.getChannel().getName());
        }
    }
}