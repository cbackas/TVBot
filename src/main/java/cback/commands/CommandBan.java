package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.*;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandBan implements Command {
    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVRoles.STAFF.id))) {
            String text = message.getContent();
            IUser mod = message.getAuthor();
            IChannel logChannel = guild.getChannelByID("217456105679224846");
            try {
               DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(mod), EnumSet.of(Permissions.BAN));
                Pattern pattern = Pattern.compile("^!ban <@!?(\\d+)> ?(.+)?");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String userInput = matcher.group(1);
                    String reason = matcher.group(2);
                    if (reason != null) {
                        IUser user = guild.getUserByID(userInput);
                        if (user.getID().equals(mod.getID())) {
                            Util.sendMessage(message.getChannel(), "You're gonna have to try harder than that.");
                        } else {
                            try {
                                guild.banUser(user, 1);
                                Util.sendLog(message, "Banned " + user.getDisplayName(guild) + "\n**Reason:** " + reason, Color.red);
                                Util.sendMessage(message.getChannel(), user.getDisplayName(guild) + " has been banned. Check " + guild.getChannelByID(TVBot.LOG_CHANNEL_ID).mention() + " for more info.");
                            } catch (Exception e) {
                                e.printStackTrace();
                                Util.sendMessage(message.getChannel(), "Internal error - cback has been notified");
                                Util.errorLog(message, "Error running CommandBan - check stacktrace");
                            }
                        }
                    } else {
                        Util.sendPrivateMessage(mod, "**Error Banning**: Reason required");
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!ban @user reason``");
                }
            } catch (Exception e) {
            }
            Util.botLog(message);
            Util.deleteMessage(message);
        }
    }

}
