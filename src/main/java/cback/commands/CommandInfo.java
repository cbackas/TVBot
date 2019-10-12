package cback.commands;

import cback.TVBot;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CommandInfo extends Command {

    private TVBot bot = TVBot.getInstance();

    public CommandInfo() {
        this.name = "info";
        this.aliases = new String[]{"serverinfo", "server", "stats", "about"};
        this.help = "Displays some statistics about the server and the bot";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        int userCount = commandEvent.getGuild().getMembers().size();
        int oldUserCount = Integer.parseInt(bot.getConfigManager().getConfigValue("userCount"));
        int channelCount = commandEvent.getGuild().getChannels().size();
        int closedChannels = commandEvent.getGuild().getCategoryById(TVBot.CLOSED_CAT_ID).getChannels().size();

        int newCount = userCount - oldUserCount;
        String leaveJoin = " (-" + bot.getConfigManager().getConfigValue("left") + " +" + bot.getConfigManager().getConfigValue("joined") + ")";
        String userChange = newCount + leaveJoin;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
        OffsetDateTime dt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofHours(0));

        EmbedBuilder embed = Util.getEmbed(commandEvent.getAuthor()).setThumbnail(Util.getAvatar(bot.getClient().getSelfUser()));
        embed.setTitle(commandEvent.getGuild().getName());
        embed.addField("Created: ", commandEvent.getGuild().getJDA().retrieveApplicationInfo().complete().getTimeCreated().format(formatter), true);
        embed.addField("Users: ", Integer.toString(userCount), true);
        embed.addField("New Users: ", userChange, true);
        embed.addField("Text Channels: ", String.valueOf(channelCount), true);
        embed.addField("Closed Channels: ", String.valueOf(closedChannels), true);

        embed.addBlankField(false);

        embed.addField("Bot Uptime: ", TVBot.getInstance().getUptime(), true);
        embed.addField("Our Servers: ", "[`The Lounge`](http://discord.me/lounge)\n[`The Cinema`](https://discord.gg/QeuTNRb)", true);
        embed.addField("Feed bot developers: ", "[`Paypal`](https://www.paypal.me/cbackas)", true);

        embed.addBlankField(false);

        embed.addField("Made By: ", Util.getTag(bot.getClient().getSelfUser()), true);
        embed.addField("Source: ", "[`GitHub`](https://github.com/cbackas/TVBot)", true);

        Util.sendEmbed(commandEvent.getChannel(), embed.setColor(Util.getBotColor()).build());
        Util.deleteMessage(commandEvent.getMessage());
    }
}
