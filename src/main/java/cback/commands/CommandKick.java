package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.*;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandKick implements Command {
    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) {
            String text = message.getContent();
            IUser mod = message.getAuthor();
            IChannel logChannel = guild.getChannelByID("217456105679224846");
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

    @Override
    public boolean isLogged() {
        return true;
    }
}
