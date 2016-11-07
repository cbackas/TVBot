package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUnEmbedMute implements Command {
    @Override
    public String getName() {
        return "unembedmute";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) {
            if (userRoles.contains(guild.getRoleByID(TVBot.TRIALMOD_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.MOD_ROLE_ID))) {
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
                                userInput.removeRole(guild.getRoleByID("239233306325942272"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + "'s embed/attach-files permission has been restored.");
                                Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + userInput.getDisplayName(guild) + "'s embed/attach-files permission has been restored.\n- " + message.getAuthor().getDisplayName(guild) + "```");
                                Util.deleteMessage(message);
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!unembedmute @user``");
                }
                Util.botLog(message);
            }
        }
    }

}
