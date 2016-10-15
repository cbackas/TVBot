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

public class CommandMute implements Command {
    @Override
    public String getName() {
        return "mute";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) {
            if (args.length == 1) {
                String user = args[0];
                Pattern pattern = Pattern.compile("^!ban <@(.+)> ?(.+)?");
                Matcher matcher = pattern.matcher(user);
                if (matcher.find()) {
                    String u = matcher.group(1);
                    String reason = matcher.group(2);
                    if (reason == null) {
                        reason = "no reason provided";
                    }
                    IUser userInput = guild.getUserByID(u);
                    if (message.getAuthor().getID().equals(u)) {
                        Util.sendMessage(message.getChannel(), "You probably shouldn't mute yourself");
                    } else {
                        try {
                            DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                            userInput.addRole(guild.getRoleByID("231269949635559424"));
                            Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been muted");
                            Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```Muted " + userInput.getDisplayName(guild) + " for " + reason + "\n- " + message.getAuthor().getDisplayName(guild) + "```");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!mute @user``");
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
