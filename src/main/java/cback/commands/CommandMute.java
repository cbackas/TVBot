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

public class CommandMute implements Command {
    @Override
    public String getName() {
        return "mute";
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
                if (args.length >= 1) {
                    String text = message.getContent();
                    Pattern pattern = Pattern.compile("^!mute <@(.+)> ?(.+)?");
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        String u = matcher.group(1);
                        String reason = matcher.group(2);
                        if (reason == null) {
                            reason = "an unspecified reason";
                        }
                        IUser userInput = guild.getUserByID(u);
                        if (message.getAuthor().getID().equals(u)) {
                            Util.sendMessage(message.getChannel(), "You probably shouldn't mute yourself");
                        } else {
                            try {
                                userInput.addRole(guild.getRoleByID("231269949635559424"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been muted");
                                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                                mutedUsers.add(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);
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
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
