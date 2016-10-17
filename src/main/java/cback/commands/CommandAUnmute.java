package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionsUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandAUnmute implements Command {
    @Override
    public String getName() {
        return "aunmute";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        try {
            PermissionsUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.ADMINISTRATOR));
            if (args.length == 1) {
                String user = args[0];
                Pattern pattern = Pattern.compile("^<@!?(\\d+)>");
                Matcher matcher = pattern.matcher(user);
                if (matcher.find()) {
                    String u = matcher.group(1);
                    IUser userInput = guild.getUserByID(u);
                    if (message.getAuthor().getID().equals(u)) {
                        Util.sendPrivateMessage(message.getAuthor(), "Not sure how you typed this command... but you can't unmute yourself");
                    } else {
                        try {
                            PermissionsUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                            userInput.removeRole(guild.getRoleByID("231269949635559424"));
                            List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                            mutedUsers.remove(u);
                            bot.getConfigManager().setConfigValue("muted", mutedUsers);
                            Util.sendPrivateMessage(message.getAuthor(), userInput.getDisplayName(guild) + " has been unmuted");
                        } catch (Exception e) {
                        }
                    }
                }
            } else {
                Util.sendPrivateMessage(message.getAuthor(), "Invalid arguments. Usage: ``!unmute @user``");
            }
        } catch (Exception p) {
            p.printStackTrace();
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
