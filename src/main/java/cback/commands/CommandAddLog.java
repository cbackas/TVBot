package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.PermissionsUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class CommandAddLog implements Command {
    @Override
    public String getName() {
        return "addlog";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("log");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) {
            if (args.length >= 1) {
                List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
                if (userRoles.contains(guild.getRoleByID(TVBot.TRIALMOD_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.MOD_ROLE_ID))) {
                    String text = message.getContent().split(" ", 2)[1];
                    List<IChannel> mentionsC = message.getChannelMentions();
                    List<IUser> mentionsU = message.getMentions();
                    List<IRole> mentionsG = message.getRoleMentions();
                    String finalText = text;
                    for (IChannel c : mentionsC) {
                        String displayName = c.getName();
                        finalText = text.replace(c.mention(), displayName).replace(c.mention(), displayName);
                    }
                    for (IUser u : mentionsU) {
                        String displayName = u.getDisplayName(guild);
                        finalText = finalText.replace(u.mention(false), displayName).replace(u.mention(true), displayName);
                    }
                    for (IRole g : mentionsG) {
                        String displayName = g.getName();
                        finalText = finalText.replace(g.mention(), displayName).replace(g.mention(), displayName);
                    }
                    Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + finalText + "\n- " + message.getAuthor().getDisplayName(guild) + "```");
                    Util.sendMessage(message.getChannel(), "Log added.");
                    Util.deleteMessage(message);
                } else {
                    Util.sendMessage(message.getChannel(), "You don't have permission to add logs.");
                }
            } else {
                Util.sendMessage(message.getChannel(), "Usage: !addlog <text>");
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
