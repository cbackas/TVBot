package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandAnnounce implements Command {
    @Override
    public String getName() {
        return "announce";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID("192441946210435072"))) {
            if (args.length >= 1) {
                String announcement = Arrays.stream(args).collect(Collectors.joining(" "));
                Util.deleteMessage(message);
                Util.sendMessage(guild.getChannelByID(TVBot.GENERAL_CHANNEL_ID), announcement);
                Util.sendMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID), announcement);
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
