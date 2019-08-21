package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;

import com.uwetrottmann.trakt5.entities.Show;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class ChannelChange {
    private TVBot bot;

    public ChannelChange(TVBot bot) {
        this.bot = bot;
    }

    //Set all
    public void setMuteRoleMASS(MessageReceivedEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            Message message = event.getMessage();
            String text = message.getContentRaw();
            JDA client = event.getJDA();
            if (text.equalsIgnoreCase("!setmuteperm") && message.getAuthor().getId().equals("73416411443113984")) {
                List<Channel> channelList = client.getGuildById(192441520178200577L).getChannels();
                Guild guild = event.getJDA().getGuildById(192441520178200577L);
                Role muted = event.getGuild().getRoleById(239233306325942272L);

                for (Channel channels : channelList) {
                    try {
                        channels.createPermissionOverride(muted).setDeny(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES).queue();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Set muted role");
                Util.deleteMessage(message);
            }
        }
    }

    //New Channel
    public void newChannel(TextChannelCreateEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            //Set muted role
            Guild guild = event.getJDA().getGuildById(192441520178200577L);
            Role muted = guild.getRoleById(231269949635559424L);

            try {
                event.getChannel().createPermissionOverride(muted).setDeny(Permission.MESSAGE_WRITE).queue();
            } catch (Exception e) {
                Util.reportHome(e);
            }

            //Check for tv show
            TraktManager trakt = bot.getTraktManager();
            String[] showNameArray = event.getChannel().getName().split("-");
            String showName = "\"" + String.join(" ", showNameArray) + "\"";
            Show showData = trakt.showSummaryFromName(showName);
            String possibleID = showData.ids.imdb;
            if (possibleID != null) {
                String nameFromIMDB = trakt.getShowTitle(possibleID);
                Util.simpleEmbed(event.getChannel(), "Found possible show: **" + nameFromIMDB + "**. The showID is: " + possibleID + "." +
                        "\n\nAdmins use: !addshow " + possibleID + " here to add the show");
            }
        }
    }


    public void onDeleteChannelEvent(TextChannelDeleteEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            List<cback.database.tv.Show> shows = bot.getDatabaseManager().getTV().getShowsByChannel(event.getChannel().getId());
            if (shows != null) {
                shows.forEach(show -> {
                    if (bot.getDatabaseManager().getTV().deleteShow(show.getShowID()) > 0) {
                        String message = "Channel Deleted: Removed show " + show.getShowName() + " from database automatically.";
                        System.out.println(message);
                        Util.simpleEmbed(bot.getClient().getTextChannelById(231499461740724224L), message);
                    }
                });
            }

            List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
            if (permChannels.contains(event.getChannel().getId())) {
                permChannels.remove(event.getChannel().getId());
                bot.getConfigManager().setConfigValue("permanentchannels", permChannels);
            }
        }
    }
}