package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSearch implements Command {
    @Override
    public String getName() {
        return "search";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("lookup");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String showName = Arrays.stream(args).collect(Collectors.joining(" "));
        Show showData = bot.getTraktManager().showSearch(showName);
        System.out.println(showName);
        if (showData != null) {
            //System.out.println(showData.first_aired.toString("MMM dd, yyyy"));
            System.out.println("not null");
            String title = "**" + showData.title + " (" + Integer.toString(showData.year) + ")**";
            String overview = showData.overview;
            //String airs = showData.airs.day + " at " + showData.airs.time + " " + showData.airs.timezone;
            //String premier = new SimpleDateFormat("MMM dd, yyyy").format(showData.first_aired.toDate());
            //String runtime = Integer.toString(showData.runtime);
            String country = showData.country + " - " + showData.language;
            String homepage = "<" + showData.homepage + ">";
            Util.sendMessage(message.getChannel(),
                    title + "\n" +
                            overview + "\n" +
                            "```\n" +
                            "AIRS: " + "airs" + "\n" +
                            "PREMIERED: " + "premier" + "\n" +
                            "RUNTIME: " + "runtime" + "min\n" +
                            "COUNTRY: " + country + "\n" +
                            "```\n" +
                            homepage);
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
