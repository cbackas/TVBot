package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.tv.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandRemoveShow implements Command {
    @Override
    public String getName() {
        return "removeshow";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (bot.getBotAdmins().contains(message.getAuthor().getID())) {
            if (args.length >= 1) {
                String showID = args[0];
                Show show = bot.getDatabaseManager().getTV().getShow(showID);
                int entriesDeleted = bot.getDatabaseManager().getTV().deleteShow(showID);
                if (show != null && entriesDeleted > 0) {
                    Util.sendMessage(message.getChannel(), "Removed show: " + show.getShowName() + ".");
                    System.out.println("@" + message.getAuthor().getName() + " removed show " + show.getShowName());
                } else {
                    Util.sendMessage(message.getChannel(), "No saved show found by this IMDB ID.");
                }
            } else {
                Util.sendMessage(message.getChannel(), "Usage: !removeshow <imdbID>");
            }
            Util.botLog(message);
        }
    }

}
