package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KickAndBan {

    @EventSubscriber
    public void modMessageEvent(MessageReceivedEvent event) {
        IMessage message = event.getMessage();
        String text = message.getContent();
        IGuild guild = message.getGuild();
        IUser mod = message.getAuthor();
        IChannel logChannel = guild.getChannelByID("209644964328636417");
        if (text.toLowerCase().startsWith("!ban")) {
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(mod), EnumSet.of(Permissions.BAN));
                Pattern pattern = Pattern.compile("^!ban <@(.+)> (.+)");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String userInput = matcher.group(1);
                    String reason = matcher.group(2);
                    IUser user = guild.getUserByID(userInput);
                    if (!user.getID().equals(mod.getID())) {
                        try {
                            guild.banUser(user, 1);
                            Util.sendMessage(logChannel, "```" + user.getDisplayName(guild) + "banned. Reason: " + reason + "\n- " + mod.getDisplayName(guild));
                            Util.deleteMessage(message);
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                Util.sendMessage(message.getChannel(), "You don't have permission to ban users");
            }
        } else if (text.toLowerCase().startsWith("!kick")) {
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(mod), EnumSet.of(Permissions.KICK));
                Pattern pattern = Pattern.compile("^!kick <@(.+)> (.+)");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String userInput = matcher.group(1);
                    String reason = matcher.group(2);
                    IUser user = guild.getUserByID(userInput);
                    if (!user.getID().equals(mod.getID())) {
                        try {
                            guild.kickUser(user);
                            Util.sendMessage(logChannel, "```" + user.getDisplayName(guild) + "kicked. Reason: " + reason + "\n- " + mod.getDisplayName(guild));
                            Util.deleteMessage(message);
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                Util.sendMessage(message.getChannel(), "You don't have permission to kick users");
            }
        }
    }
}
