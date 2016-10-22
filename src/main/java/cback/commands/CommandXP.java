package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

public class CommandXP implements Command {
    @Override
    public String getName() {
        return "xp";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        IUser user;
        if (args.length >= 1) {
            user = Util.getUserFromMentionArg(args[0]);
        }else{
            user = message.getAuthor();
        }

        if (user != null) {
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getID());
            if (xp != null) {
                Util.sendMessage(message.getChannel(), "XP for " + user.getDisplayName(guild) + ": ``" + xp.getMessageCount() + "``");
            } else {
                Util.sendMessage(message.getChannel(), "No xp data found for " + user.getDisplayName(guild));
            }
        } else {
            Util.sendMessage(message.getChannel(), "Invalid user \"" + args[0] + "\".");
        }

        Util.deleteMessage(message);
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
