package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;

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

    /*@EventSubscriber //Set all
    public void setMuteRoleMASS(MessageReceivedEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            IMessage message = event.getMessage();
            String text = message.getContent();
            IDiscordClient client = event.getClient();
            if (text.equalsIgnoreCase("!setmuteperm") && message.getAuthor().getStringID().equals("73416411443113984")) {
                List<IChannel> channelList = client.getGuildByID(192441520178200577l).getChannels();
                IGuild guild = event.getClient().getGuildByID(192441520178200577l);
                IRole muted = guild.getRoleByID(239233306325942272l);
                for (IChannel channels : channelList) {
                    RequestBuffer.request(() -> {
                        try {
                            channels.overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.EMBED_LINKS, Permissions.ATTACH_FILES));
                        } catch (MissingPermissionsException | DiscordException e) {
                            e.printStackTrace();
                        }
                    });
                }
                System.out.println("Set muted role");
                Util.deleteMessage(message);
            }
        }
    }

    @EventSubscriber //New Channel
    public void newChannel(ChannelCreateEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            //Set muted role
            IGuild guild = event.getClient().getGuildByID(192441520178200577l);
            IRole muted = guild.getRoleByID(231269949635559424l);

            RequestBuffer.request(() -> {
                try {
                    event.getChannel().overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.SEND_MESSAGES));
                } catch (MissingPermissionsException | DiscordException e) {
                    Util.reportHome(e);
                }
            });

            //Check for tv show
            TraktManager trakt = bot.getTraktManager();
            String[] showNameArray = event.getChannel().getName().split("-");
            String showName = "\"" + Arrays.stream(showNameArray).collect(Collectors.joining(" ")) + "\"";
            Show showData = trakt.showSummaryFromName(showName);
            String possibleID = showData.ids.imdb;
            if (possibleID != null) {
                String nameFromIMDB = trakt.getShowTitle(possibleID);
                Util.simpleEmbed(event.getChannel(), "Found possible show: **" + nameFromIMDB + "**. The showID is: " + possibleID + "." +
                        "\n\nAdmins use: !addshow " + possibleID + " here to add the show");
            }
        }
    }

    @EventSubscriber
    public void onDeleteChannelEvent(ChannelDeleteEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            List<cback.database.tv.Show> shows = bot.getDatabaseManager().getTV().getShowsByChannel(event.getChannel().getStringID());
            if (shows != null) {
                shows.forEach(show -> {
                    if (bot.getDatabaseManager().getTV().deleteShow(show.getShowID()) > 0) {
                        String message = "Channel Deleted: Removed show " + show.getShowName() + " from database automatically.";
                        System.out.println(message);
                        Util.simpleEmbed(bot.getClient().getChannelByID(231499461740724224l), message);
                    }
                });
            }

            List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
            if (permChannels.contains(event.getChannel().getStringID())) {
                permChannels.remove(event.getChannel().getStringID());
                bot.getConfigManager().setConfigValue("permanentchannels", permChannels);
            }
        }
    }*/

}
