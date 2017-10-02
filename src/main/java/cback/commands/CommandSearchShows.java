package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSearchShows implements Command {
    @Override
    public String getName() {
        return "show";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "show [show name]";
    }

    @Override
    public String getDescription() {
        return "Searches trakt.tv for the provided show. Sometimes it works, sometimes it doesn't.";
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        String showName = Arrays.stream(args).collect(Collectors.joining(" "));

        IMessage initialMessage = Util.simpleEmbed(message.getChannel(), "Searching <https://trakt.tv/> for " + showName + " ...");

        Show showData = bot.getTraktManager().showSummaryFromName(showName);
        if (showData != null) {
            String title = showData.title + " (" + Integer.toString(showData.year) + ") ";
            String overview = showData.overview;
            String airs = (showData.status == Status.RETURNING || showData.status == Status.IN_PRODUCTION)
                    ? showData.airs.day + " at " + Util.to12Hour(showData.airs.time) + " EST on " + showData.network : "Ended";
            String premier = new SimpleDateFormat("MMM dd, yyyy").format(new Date(showData.first_aired.toInstant().toEpochMilli()));
            String runtime = Integer.toString(showData.runtime);
            String country = showData.country + " - " + showData.language;
            String homepage = "<https://trakt.tv/shows/" + showData.ids.slug + ">\n<http://www.imdb.com/title/" + showData.ids.imdb + ">";

            try {
                overview = guild.getChannelByID(Long.parseLong(bot.getDatabaseManager().getTV().getShow(showData.ids.imdb).getChannelID())).mention() + "\n" + overview;
            } catch (Exception ignored) {
            }

            EmbedBuilder embed = Util.getEmbed(message.getAuthor());

            embed.withTitle(title);
            embed.withDescription(overview);
            embed.appendField("References:", homepage, false);
            embed.appendField("AIRS:", airs, true);
            embed.appendField("RUNTIME:", runtime, true);
            embed.appendField("PREMIERED:", premier, true);
            embed.appendField("COUNTRY:", country.toUpperCase(), true);
            embed.appendField("GENRES:", String.join(", ", showData.genres), true);

            initialMessage.edit(embed.withColor(Util.getBotColor()).build());
        } else {
            initialMessage.edit(new EmbedBuilder().withDesc("Error: Show not found").withColor(Util.getBotColor()).build());
            Util.simpleEmbed(client.getChannelByID(TVBot.ERRORLOG_CH_ID), "Couldn't find show " + showName + " in " + guild.getName() + "/" + message.getChannel().getName());
        }
    }

}

