package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

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
        return Arrays.asList("serverinfo", "server", "stats", "about");
    }

    @Override
    public String getSyntax() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Displays some statistics about the server and the bot";
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        int userCount = guild.getUsers().size();
        int oldUserCount = Integer.valueOf(bot.getConfigManager().getConfigValue("userCount"));
        int channelCount = guild.getChannels().size();

        int newCount = userCount - oldUserCount;
        String leaveJoin = " (-" + bot.getConfigManager().getConfigValue("left") + " +" + bot.getConfigManager().getConfigValue("joined") + ")";
        String userChange = newCount + leaveJoin;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

        EmbedBuilder embed = Util.getEmbed(message.getAuthor()).withThumbnail(Util.getAvatar(client.getOurUser()));
        embed.withTitle(guild.getName());
        embed.appendField("Created: ", guild.getCreationDate().format(formatter), true);

        embed.appendField("\u200B", "\u200B", false);

        embed.appendField("Users: ", Integer.toString(userCount), true);
        embed.appendField("New Users: ", userChange, true);
        embed.appendField("Text Channels: ", String.valueOf(channelCount), true);

        embed.appendField("\u200B", "\u200B", false);

        embed.appendField("Bot Uptime: ", TVBot.getInstance().getUptime(), true);
        embed.appendField("Our Servers: ", "[`The Lounge`](http://discord.me/lounge)\n[`The Cinema`](https://discord.gg/QeuTNRb)\n[`The Arcade`](discord.gg/Empn64q)", true);
        embed.appendField("Feed bot developers: ", "[`paypal.me`](paypal.me/cbackas)", true);

        embed.appendField("\u200B", "\u200B", false);

        embed.appendField("Made By: ", "cback#3986", true);
        embed.appendField("Source: ", "[`GitHub`](https://github.com/cbackas/TVBot)", true);

        Util.sendEmbed(message.getChannel(), embed.withColor(023563).build());
        Util.deleteMessage(message);
    }
}
