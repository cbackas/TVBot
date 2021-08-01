package cback.commandsV2;

import cback.TVBot;
import cback.Util;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CommandInfo extends Command {
    public CommandInfo() {
        super("info", "Displays some statistics about the server and the bot");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                .flatMap(v ->
                        event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                ).queue(); // Queue both reply and edit
        System.out.println("executed");

//        try {
//            int userCount = event.getGuild().getMembers().size();
//            int oldUserCount = Integer.parseInt(bot.getConfigManager().getConfigValue("userCount"));
//            int channelCount = event.getGuild().getChannels().size();
//            int closedChannels = event.getGuild().getCategoryById(TVBot.CLOSED_CAT_ID).getChannels().size();
//
//            int newCount = userCount - oldUserCount;
//            String leaveJoin = " (-" + bot.getConfigManager().getConfigValue("left") + " +" + bot.getConfigManager().getConfigValue("joined") + ")";
//            String userChange = newCount + leaveJoin;
//
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
//            OffsetDateTime dt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofHours(0));
//
//            EmbedBuilder embed = Util.getEmbed(event.getUser()).setThumbnail(Util.getAvatar(bot.getClient().getSelfUser()));
//            embed.setTitle(event.getGuild().getName());
//            embed.addField("Created: ", event.getGuild().getJDA().retrieveApplicationInfo().complete().getTimeCreated().format(formatter), true);
//            embed.addField("Users: ", Integer.toString(userCount), true);
//            embed.addField("New Users: ", userChange, true);
//            embed.addField("Text Channels: ", String.valueOf(channelCount), true);
//            embed.addField("Closed Channels: ", String.valueOf(closedChannels), true);
//
//            embed.addBlankField(false);
//
//            embed.addField("Bot Uptime: ", TVBot.getInstance().getUptime(), true);
//            embed.addField("Our Servers: ", "[`The Lounge`](http://discord.me/lounge)\n[`The Cinema`](https://discord.gg/QeuTNRb)", true);
//            embed.addField("Feed bot developers: ", "[`Paypal`](https://www.paypal.me/cbackas)", true);
//
//            embed.addBlankField(false);
//
//            embed.addField("Made By: ", Util.getTag(bot.getClient().getSelfUser()), true);
//            embed.addField("Source: ", "[`GitHub`](https://github.com/cbackas/TVBot)", true);
//            MessageEmbed infoEmbed = embed.setColor(Util.getBotColor()).build();
//            event.getHook().sendMessage("test").queue();
//
//        } catch (Exception e) {
//            System.out.println(e);
//        }
    }
}
