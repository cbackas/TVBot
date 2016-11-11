package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        int userCount = guild.getUsers().size();
        int oldUserCount = Integer.valueOf(bot.getConfigManager().getConfigValue("userCount"));
        int channelCount = guild.getChannels().size();

        int newCount = userCount - oldUserCount;
        String leaveJoin = " (-" + bot.getConfigManager().getConfigValue("left") + " +" + bot.getConfigManager().getConfigValue("joined") + ")";
        String userChange = newCount + leaveJoin;

        int suggestionCount = Util.getSuggestions().size();

        String words =
                "**" + serverName + "**" +
                        "\n``" + "Created Jun 14, 2016" + "``" +
                        "\n\n``Users:`` " + userCount +
                        "\n``New Users:`` " + userChange +
                        "\n``Channels:`` " + channelCount +
                        "\n``Suggestions:`` " + suggestionCount;

        Util.deleteMessage(message);
        Util.sendMessage(message.getChannel(), words);
    }

}
