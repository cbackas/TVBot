package cback.commands;

import cback.Util;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KickAndBan {

    @EventSubscriber
    public void kickBanCommands(MessageReceivedEvent event) {
        IMessage message = event.getMessage();
        String text = message.getContent();
        IGuild guild = message.getGuild();
        IUser mod = message.getAuthor();
        IChannel logChannel = guild.getChannelByID("217456105679224846");
        if (text.toLowerCase().startsWith("!ban")) {
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(mod), EnumSet.of(Permissions.BAN));
                Pattern pattern = Pattern.compile("^!ban <@(.+)> ?(.+)?");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String userInput = matcher.group(1);
                    String reason = matcher.group(2);
                    if (reason == null) {
                        reason = "no reason provided";
                    }
                    IUser user = guild.getUserByID(userInput);
                    if (!user.getID().equals(mod.getID())) {
                        try {
                            guild.banUser(user, 1);
                            Util.sendMessage(logChannel, "```" + user.getDisplayName(guild) + " banned. Reason: " + reason + "\n- " + mod.getDisplayName(guild) + "```");
                            Util.sendMessage(message.getChannel(), user.getDisplayName(guild) + " banned and logged");
                            Util.deleteMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Util.sendMessage(message.getChannel(), "Internal error - check stack trace");
                        }
                    } else {
                        Util.sendMessage(message.getChannel(), "You probably shouldn't ban yourself");
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!ban @user reason``");
                }
            } catch (Exception e) {
            }
        } else if (text.toLowerCase().startsWith("!kick")) {
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(mod), EnumSet.of(Permissions.KICK));
                Pattern pattern = Pattern.compile("^!kick <@(.+)> ?(.+)?");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String userInput = matcher.group(1);
                    String reason = matcher.group(2);
                    if (reason == null) {
                        reason = "no reason provided";
                    }
                    IUser user = guild.getUserByID(userInput);
                    if (!user.getID().equals(mod.getID())) {
                        try {
                            guild.kickUser(user);
                            Util.sendMessage(logChannel, "```" + user.getDisplayName(guild) + " kicked. Reason: " + reason + "\n- " + mod.getDisplayName(guild) + "```");
                            Util.sendMessage(message.getChannel(), user.getDisplayName(guild) + " kicked and logged");
                            Util.deleteMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Util.sendMessage(message.getChannel(), "Internal error - check stack trace");
                        }
                    } else {
                        Util.sendMessage(message.getChannel(), "You probably shouldn't kick yourself");
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!kick @user reason``");
                }
            } catch (Exception e) {
            }
        }
    }
}
