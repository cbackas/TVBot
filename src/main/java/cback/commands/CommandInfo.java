package cback.commands;

import cback.TVBot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;

public class CommandInfo implements Command {
    @Override
    public String getName() {
        return "info";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("serverinfo","server");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String serverName = guild.getName();
        String userCount = Integer.toString(guild.getUsers().size());
        String channelCount = Integer.toString(guild.getChannels().size());
        String botResponseTime = Long.toString(client.getResponseTime());
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
