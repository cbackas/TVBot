package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUnmute implements Command {
    @Override
    public String getName() {
        return "unmute";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length == 1) {
            String user = args[0];
            Pattern pattern = Pattern.compile("^<@(.+)>");
            Matcher matcher = pattern.matcher(user);
            if (matcher.find()) {
                String u = matcher.group(1);
                IUser userInput = guild.getUserByID(u);
                if (message.getAuthor().getID().equals(u)) {
                    Util.sendMessage(message.getChannel(), "Not sure how you typed this command... but you can't unmute yourself");
                } else {
                    try {
                        DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                        userInput.removeRole(guild.getRoleByID("231269949635559424"));
                        Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been unmuted");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Util.sendMessage(message.getChannel(), "You don't have permission to mute members or that user can't be muted");
                    }
                }
            }
        } else {
            Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!unmute @user``");
        }
    }
}
