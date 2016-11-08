package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

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
            if (userRoles.contains(guild.getRoleByID(TVBot.TRIALMOD_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.MOD_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.REDDITMOD_ROLE_ID))) {
                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");

                if (args[0].equalsIgnoreCase("list")) {

                    StringBuilder list = new StringBuilder();
                    for (String userID : mutedUsers) {

                        IUser user =guild.getUserByID(userID);

                        list.append("\n").append(user.mention());
                    }

                    Util.sendMessage(message.getChannel(), "**Muted Users**:\n" + list.toString());

                }

                else if (args.length >= 1) {
                    String text = message.getContent();

                    Pattern pattern = Pattern.compile("^!mute <@!?(\\d+)> ?(.+)?");
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
                        }

                        else {
                            try {
                                userInput.addRole(guild.getRoleByID("231269949635559424"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been muted. Check " + guild.getChannelByID(TVBot.LOG_CHANNEL_ID).mention() + " for more info.");

                                mutedUsers.add(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);

                                Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```Muted " + userInput.getDisplayName(guild) + " for " + reason + ".\n- " + message.getAuthor().getDisplayName(guild) + "```");
                                Util.deleteMessage(message);
                            } catch (Exception e) {
                                e.printStackTrace();

                                Util.sendMessage(message.getChannel(), "Internal error - cback has been notified");
                                Util.errorLog(message, "Error running CommandBan - check stacktrace");
                            }
                        }
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!mute @user``");
                }
                Util.botLog(message);
            }
        }
    }

}
