package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSearch implements Command {
    @Override
    public String getName() {
        return "search";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("lookup", "show");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String showName = Arrays.stream(args).collect(Collectors.joining(" "));
        Show showData = bot.getTraktManager().showSummaryFromName(showName);
        if (showData != null) {
            String title = "**" + showData.title + " (" + Integer.toString(showData.year) + ")**";
            String overview = showData.overview;
            String airs = (showData.status == Status.RETURNING || showData.status == Status.IN_PRODUCTION)
                    ? showData.airs.day + " at " + to12Hour(showData.airs.time) + " EST on " + showData.network : "Ended";
            String premier = new SimpleDateFormat("MMM dd, yyyy").format(showData.first_aired.toDate());
            String runtime = Integer.toString(showData.runtime);
            String country = showData.country + " - " + showData.language;
            String homepage = "<https://trakt.tv/shows/" + showData.ids.slug + ">";
            Util.sendMessage(message.getChannel(),
                    title + "\n" +
                            overview + "\n" +
                            homepage + "\n" +
                            "```\n" +
                            "AIRS: " + airs + "\n" +
                            "RUNTIME: " + runtime + " min\n" +
                            "PREMIERED: " + premier + "\n" +
                            "COUNTRY: " + country.toUpperCase() + "\n" +
                            "GENRES: " + String.join(", ", showData.genres) + "\n" +
                            "```\n");
        } else {
            Util.sendMessage(message.getChannel(), "Error: Show not found");
            Util.sendPrivateMessage(client.getUserByID("73416411443113984"), "Couldn't find " + showName);
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }

    public static String to12Hour(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            Date dateObj = sdf.parse(time);
            return new SimpleDateFormat("K:mm").format(dateObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }
}

