package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import java.util.Arrays;
import java.util.List;

public class CommandLog implements Command {
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
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVRoles.STAFF.id))) {

            Util.botLog(message);

            if (args.length >= 1) {
                List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
                if (userRoles.contains(guild.getRoleByID(TVRoles.TRIALMOD.id)) || userRoles.contains(guild.getRoleByID(TVRoles.ADMIN.id)) || userRoles.contains(guild.getRoleByID(TVRoles.MOD.id))) {
                    String finalText = message.getFormattedContent().split(" ", 2)[1];
                    Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + finalText + "\n- " + message.getAuthor().getDisplayName(guild) + "```");
                    Util.sendMessage(message.getChannel(), "Log added. " + guild.getChannelByID(TVBot.LOG_CHANNEL_ID).mention());
                    Util.deleteMessage(message);
                } else {
                    Util.sendMessage(message.getChannel(), "You don't have permission to add logs.");
                }
            } else {
                Util.sendMessage(message.getChannel(), "Usage: !addlog <text>");
            }
        }
    }

}
