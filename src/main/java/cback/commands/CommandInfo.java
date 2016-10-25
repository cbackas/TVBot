package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class CommandInfo implements Command {
    @Override
    public String getName() {
        return "info";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("serverinfo", "server", "stats");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String serverName = guild.getName();
        String creationDate = new SimpleDateFormat("MMM dd, yyyy").format(guild.getCreationDate());
        int userCount = guild.getUsers().size();
        int oldUserCount = Integer.valueOf(bot.getConfigManager().getConfigValue("userCount"));
        int newCount = userCount - oldUserCount;
        int channelCount = guild.getChannels().size();
        long botResponseTime = client.getResponseTime();

        String words =
                "**" + serverName + "**" +
                        "\n``Created " + creationDate + "``" +
                        "\n\n``Users:`` " + userCount +
                        "\n``New Users:`` " + newCount +
                        "\n``Channels:`` " + channelCount;
        Util.deleteMessage(message);
        Util.sendMessage(message.getChannel(), words);
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
