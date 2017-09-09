package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMuteAdd implements Command {
    @Override
    public String getName() {
        return "mute";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "mute @user [reason?]";
    }

    @Override
    public String getDescription() {
        return "Mutes a user and logs the action";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.STAFF.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");

        if (args[0].equalsIgnoreCase("list")) {

            StringBuilder mutedList = new StringBuilder();
            if (!mutedUsers.isEmpty()) {
                for (String userID : mutedUsers) {

                    IUser userO = guild.getUserByID(Long.parseLong(userID));

                    String user = "<@" + userID + ">";
                    if (userO != null) {
                        user = userO.mention();
                    }

                    mutedList.append("\n").append(user);
                }
            } else {
                mutedList.append("\n").append("There are currently no muted users.");
            }

            Util.simpleEmbed(message.getChannel(), "Muted Users: (plain text for users not on server)\n" + mutedList.toString());

        } else if (args.length >= 1) {
            Pattern pattern = Pattern.compile("^!mute <@!?(\\d+)> ?(.+)?");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String u = matcher.group(1);
                String reason = matcher.group(2);

                IUser userInput = guild.getUserByID(Long.parseLong(u));
                if (userInput != null) {
                    if (reason == null) {
                        reason = "an unspecified reason";
                    }

                    if (message.getAuthor().getStringID().equals(u)) {
                        Util.simpleEmbed(message.getChannel(), "You probably shouldn't mute yourself");
                    } else {
                        try {
                            userInput.addRole(guild.getRoleByID(231269949635559424l));
                            Util.simpleEmbed(message.getChannel(), userInput.getDisplayName(guild) + " has been muted. Check " + guild.getChannelByID(TVBot.LOG_CHANNEL_ID).mention() + " for more info.");

                            if (!mutedUsers.contains(u)) {
                                mutedUsers.add(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);
                            }

                            Util.sendLog(message, "Muted " + userInput.getDisplayName(guild) + "\n**Reason:** " + reason, Color.gray);
                        } catch (Exception e) {
                            Util.simpleEmbed(message.getChannel(), "Error running " + this.getName() + " - error recorded");
                            Util.reportHome(message, e);
                        }
                    }
                }
            }
        } else {
            Util.syntaxError(this, message);
        }
        Util.deleteMessage(message);
    }

}
