package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.tv.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;

public class CommandShowRemove implements Command {
    @Override
    public String getName() {
        return "removeshow";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "removeshow [imdbID]";
    }

    @Override
    public String getDescription() {
        return "Removes a show from the database and disassociates it from any channels";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.HEADMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length >= 1) {
            String showID = args[0];
            Show show = bot.getDatabaseManager().getTV().getShow(showID);
            int entriesDeleted = bot.getDatabaseManager().getTV().deleteShow(showID);
            if (show != null && entriesDeleted > 0) {
                Util.sendMessage(message.getChannel(), "Removed show: " + show.getShowName() + ".");
                System.out.println("@" + message.getAuthor().getName() + " removed show " + show.getShowName());
                Util.simpleEmbed(client.getChannelByID(Long.parseLong(TVBot.getConfigManager().getConfigValue("COMMANDLOG_ID"))), show.getShowName() + " removed from the database.");
            } else {
                Util.simpleEmbed(message.getChannel(), "No saved show found by this IMDB ID.");
            }
        } else {
            Util.syntaxError(this, message);
        }
        Util.deleteMessage(message);
    }

}
