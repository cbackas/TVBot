package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.*;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandBan implements Command {
    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String text = message.getContent();
        IUser mod = message.getAuthor();
        IChannel logChannel = guild.getChannelByID("217456105679224846");
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
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
