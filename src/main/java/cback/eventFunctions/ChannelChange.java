package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelChange {
    private TVBot bot;

    public ChannelChange(TVBot bot) {
        this.bot = bot;
    }

    @EventSubscriber //Set all
    public void setMuteRoleMASS(MessageReceivedEvent event) {
        IMessage message = event.getMessage();
        String text = message.getContent();
        IDiscordClient client = event.getClient();
        if (text.equalsIgnoreCase("!setmuteperm") && message.getAuthor().getID().equals("73416411443113984")) {
            List<IChannel> channelList = client.getGuildByID("192441520178200577").getChannels();
            IRole muted = client.getRoleByID("231269949635559424");
            for (IChannel channels : channelList) {
                RequestBuffer.request(() -> {
                    try {
                        channels.overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.SEND_MESSAGES));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            System.out.println("Set muted role");
            Util.deleteMessage(message);
        }
    }

    @EventSubscriber //New Channel
    public void newChannel(ChannelCreateEvent event) {
        //Set muted role
        IRole muted = event.getClient().getRoleByID("231269949635559424");
        try {
            event.getChannel().overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class) ,EnumSet.of(Permissions.SEND_MESSAGES));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Check for tv show
        TraktManager trakt = bot.getTraktManager();
        String channelName = event.getChannel().getName();
        String[] showNameArray = channelName.split("-");
        String showName = Arrays.stream(showNameArray).collect(Collectors.joining(" "));
        Show showData = trakt.showSummaryFromName(showName);
        String possibleID = showData.ids.imdb;
        if (possibleID != null) {
            String showNetwork = showData.network;
            if (!showNetwork.equalsIgnoreCase("netflix")) {
                String nameFromIMDB = trakt.getShowTitle(possibleID);
                Util.sendMessage(event.getChannel(), "Found possible show: **" + nameFromIMDB + "**. The showID is ``" + possibleID + "``." +
                        "\n\nBot Admins use: ``!addshow " + possibleID + " here`` to add the show");
            } else {
                Util.sendMessage(event.getChannel(), "Netflix show detected - data not stored");
            }
        }
    }

    @EventSubscriber
    public void onDeleteChannelEvent(ChannelDeleteEvent event) {
        List<cback.database.Show> shows = bot.getDatabaseManager().getShowsByChannel(event.getChannel().getID());
        if (shows != null) {
            shows.forEach(show -> {
                if (bot.getDatabaseManager().deleteShow(show.getShowID()) > 0) {
                    String message = "Channel Deleted: Removed show " + show.getShowName() + " from database automatically.";
                    System.out.println(message);
                    Util.sendMessage(bot.getClient().getChannelByID("231499461740724224"), message);
                }
            });
        }
    }

}
