package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.PermissionsUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUnmute implements Command {
    @Override
    public String getName() {
        return "unmute";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) {
            //trial mod - mod - admin
            if (userRoles.contains(guild.getRoleByID("228231762113855489")) || userRoles.contains(guild.getRoleByID("192442068981776384")) || userRoles.contains(guild.getRoleByID("192441946210435072"))) {
                if (args.length == 1) {
                    String user = args[0];
                    Pattern pattern = Pattern.compile("^<@!?(\\d+)>");
                    Matcher matcher = pattern.matcher(user);
                    if (matcher.find()) {
                        String u = matcher.group(1);
                        IUser userInput = guild.getUserByID(u);
                        if (message.getAuthor().getID().equals(u)) {
                            Util.sendMessage(message.getChannel(), "Not sure how you typed this command... but you can't unmute yourself");
                        } else {
                            try {
                                userInput.removeRole(guild.getRoleByID("231269949635559424"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been unmuted");
                                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                                mutedUsers.remove(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);
                                Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + userInput.getDisplayName(guild) + " has been unmuted\n- " + message.getAuthor().getDisplayName(guild) + "```");
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!unmute @user``");
                }
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
