package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
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
        //Lounge Command Only
        if (event.getMessage().getGuild().getID().equals("192441520178200577")) {
            IMessage message = event.getMessage();
            String text = message.getContent();
            IDiscordClient client = event.getClient();
            if (text.equalsIgnoreCase("!setmuteperm") && message.getAuthor().getID().equals("73416411443113984")) {
                List<IChannel> channelList = client.getGuildByID("192441520178200577").getChannels();
                IGuild guild = event.getClient().getGuildByID("192441520178200577");
                IRole muted = guild.getRoleByID("239233306325942272");
                for (IChannel channels : channelList) {
                    RequestBuffer.request(() -> {
                        try {
                            channels.overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.EMBED_LINKS, Permissions.ATTACH_FILES));
                        } catch (Exception e) {
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
        //Lounge Command Only
        if (event.getChannel().getGuild().getID().equals("192441520178200577")) {

            //Set muted role
            IGuild guild = event.getClient().getGuildByID("192441520178200577");
            IRole muted = guild.getRoleByID("231269949635559424");
            IRole embedMuted = guild.getRoleByID("239233306325942272");

            try {
                event.getChannel().overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.SEND_MESSAGES));
                event.getChannel().overrideRolePermissions(embedMuted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.EMBED_LINKS, Permissions.ATTACH_FILES));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Check for tv show
            TraktManager trakt = bot.getTraktManager();
            String[] showNameArray = event.getChannel().getName().split("-");
            String showName = "\"" + Arrays.stream(showNameArray).collect(Collectors.joining(" ")) + "\"";
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
    }

    @EventSubscriber
    public void onDeleteChannelEvent(ChannelDeleteEvent event) {
            List<cback.database.tv.Show> shows = bot.getDatabaseManager().getTV().getShowsByChannel(event.getChannel().getID());
            if (shows != null) {
                shows.forEach(show -> {
                    if (bot.getDatabaseManager().getTV().deleteShow(show.getShowID()) > 0) {
                        String message = "**Channel Deleted: Removed show** ``" + show.getShowName() + "`` **from database automatically.**";
                        System.out.println(message);
                        Util.sendMessage(bot.getClient().getChannelByID("231499461740724224"), message);
                    }
                });
            }

            List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
            if (permChannels.contains(event.getChannel().getID())) {
                permChannels.remove(event.getChannel().getID());
                bot.getConfigManager().setConfigValue("permanentchannels", permChannels);
            }
    }

}
