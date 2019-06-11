package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelChange {
    private TVBot bot;

    public ChannelChange(TVBot bot) {
        this.bot = bot;
    }

    //Set all
    public void setMuteRoleMASS(MessageReceivedEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            Message message = event.getMessage();
            String text = message.getContentRaw();
            JDAClient client = event.getJDA().asClient();
            if (text.equalsIgnoreCase("!setmuteperm") && message.getAuthor().getStringID().equals("73416411443113984")) {
                List<Channel> channelList = client.getGuildByID(192441520178200577L).getChannels();
                Guild guild = event.getClient().getGuildByID(192441520178200577L);
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
    public void newChannel(ChannelCreateEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            //Set muted role
            IGuild guild = event.getClient().getGuildByID(192441520178200577L);
            IRole muted = guild.getRoleByID(231269949635559424L);

            RequestBuffer.request(() -> {
                try {
                    event.getChannel().overrideRolePermissions(muted, EnumSet.noneOf(Permission.class), EnumSet.of(Permission.MESSAGE_WRITE));
                } catch (Exception e) {
                    Util.reportHome(e);
                }
            });

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


    public void onDeleteChannelEvent(ChannelDeleteEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            List<cback.database.tv.Show> shows = bot.getDatabaseManager().getTV().getShowsByChannel(event.getChannel().getStringID());
            if (shows != null) {
                shows.forEach(show -> {
                    if (bot.getDatabaseManager().getTV().deleteShow(show.getShowID()) > 0) {
                        String message = "Channel Deleted: Removed show " + show.getShowName() + " from database automatically.";
                        System.out.println(message);
                        Util.simpleEmbed(bot.getClient().getChannelByID(231499461740724224L), message);
                    }
                });
            }

            List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
            if (permChannels.contains(event.getChannel().getStringID())) {
                permChannels.remove(event.getChannel().getStringID());
                bot.getConfigManager().setConfigValue("permanentchannels", permChannels);
            }
        }
    }

}
