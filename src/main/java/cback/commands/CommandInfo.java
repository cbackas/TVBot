package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.EmbedBuilder;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class CommandInfo extends Command {

    private TVBot bot;

    public CommandInfo(TVBot bot) {
        this.bot = bot;
        this.name = "info";
        this.aliases = new String[]{"serverinfo", "server", "stats", "about"};
        this.help = "Displays some statistics about the server and the bot";
        this.userPermissions = null;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        int userCount = commandEvent.getGuild().getMembers().size();
        int oldUserCount = Integer.valueOf(bot.getConfigManager().getConfigValue("userCount"));
        int channelCount = commandEvent.getGuild().getChannels().size();
        int closedChannels = commandEvent.getGuild().getCategoryById(TVBot.CLOSED_CAT_ID).getChannels().size();

        int newCount = userCount - oldUserCount;
        String leaveJoin = " (-" + bot.getConfigManager().getConfigValue("left") + " +" + bot.getConfigManager().getConfigValue("joined") + ")";
        String userChange = newCount + leaveJoin;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

        EmbedBuilder embed = Util.getEmbed(commandEvent.getAuthor()).setThumbnail(Util.getAvatar(TVBot.getClient().getJDA().getSelfUser()))
    }

    /*public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        int userCount = guild.getUsers().size();
        int oldUserCount = Integer.valueOf(bot.getConfigManager().getConfigValue("userCount"));
        int channelCount = guild.getChannels().size();
        int closedChannels = guild.getCategoryByID(TVBot.CLOSED_CAT_ID).getChannels().size();

        int newCount = userCount - oldUserCount;
        String leaveJoin = " (-" + bot.getConfigManager().getConfigValue("left") + " +" + bot.getConfigManager().getConfigValue("joined") + ")";
        String userChange = newCount + leaveJoin;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

        EmbedBuilder embed = Util.getEmbed(message.getAuthor()).withThumbnail(Util.getAvatar(client.getOurUser()));
        embed.withTitle(guild.getName());
        embed.appendField("Created: ", guild.getCreationDate().atOffset(ZoneOffset.ofHours(0)).format(formatter), true);

        embed.appendField("\u200B", "\u200B", false);

        embed.appendField("Users: ", Integer.toString(userCount), true);
        embed.appendField("New Users: ", userChange, true);
        embed.appendField("Text Channels: ", String.valueOf(channelCount), true);
        embed.appendField("Closed Channels: ", String.valueOf(closedChannels), true);

        embed.appendField("\u200B", "\u200B", false);

        embed.appendField("Bot Uptime: ", TVBot.getInstance().getUptime(), true);
        embed.appendField("Our Servers: ", "[`The Lounge`](http://discord.me/lounge)\n[`The Cinema`](https://discord.gg/QeuTNRb)", true);
        embed.appendField("Feed bot developers: ", "[`Paypal`](https://www.paypal.me/cbackas)", true);

        embed.appendField("\u200B", "\u200B", false);

        embed.appendField("Made By: ", Util.getTag(TVBot.getClient().getApplicationOwner()), true);
        embed.appendField("Source: ", "[`GitHub`](https://github.com/cbackas/TVBot)", true);

        Util.sendEmbed(message.getChannel(), embed.withColor(Util.getBotColor()).build());
        Util.deleteMessage(message);
    }*/
}
