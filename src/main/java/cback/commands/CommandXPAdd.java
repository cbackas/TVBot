package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.xp.UserXP;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;

public class CommandXPAdd implements Command {
    @Override
    public String getName() {
        return "addxp";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "addxp @user #";
    }

    @Override
    public String getDescription() {
        return "Adds a certain number of xp to the desired user.";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length >= 1) {
            IUser mentioned = Util.getUserFromMentionArg(args[0]);
            if (mentioned != null) {
                UserXP xp = bot.getDatabaseManager().getXP().getUserXP(mentioned.getStringID());
                if (xp != null) {

                    int number = Integer.parseInt(args[1]);

                    bot.getDatabaseManager().getXP().addXP(mentioned.getStringID(), number);
                    Util.simpleEmbed(message.getChannel(), "Granted " + number + " xp to " + mentioned.getDisplayName(guild));
                } else {
                    Util.simpleEmbed(message.getChannel(), "No xp data found for " + mentioned.getDisplayName(guild));
                }
            } else {
                Util.simpleEmbed(message.getChannel(), "Invalid user \"" + args[0] + "\".");
            }
        } else {
            Util.syntaxError(this, message);
        }
        Util.deleteMessage(message);
    }
}
