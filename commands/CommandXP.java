package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;

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
    public String getSyntax() {
        return "xp [@user]";
    }

    @Override
    public String getDescription() {
        return "Shows you your xp or the person you mentioned's xp";
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        IUser user;
        if (args.length >= 1) {
            user = Util.getUserFromMentionArg(args[0]);
        }else{
            user = author;
        }

        if (user != null) {
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getStringID());
            if (xp != null) {
                Util.simpleEmbed(message.getChannel(), "**" + user.getDisplayName(guild) + "** ( Rank **#" + xp.getRank() + "** )\nXP: `" + xp.getMessageCount() + "`");
            } else {
                Util.simpleEmbed(message.getChannel(), "No xp data found for " + user.getDisplayName(guild));
            }
        } else {
            Util.simpleEmbed(message.getChannel(), "Invalid user \"" + args[0] + "\". You must use an @mention");
        }
        Util.deleteMessage(message);
    }

}
