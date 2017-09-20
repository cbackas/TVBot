package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

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

    @EventSubscriber //Set all
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
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            //Set muted role
            IGuild guild = event.getClient().getGuildByID(192441520178200577l);
            IRole muted = guild.getRoleByID(231269949635559424l);

            try {
                event.getChannel().overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.SEND_MESSAGES));
            } catch (Exception e) {
                Util.reportHome(e);
            }

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

                //Builds a show embed thing
                String title = showData.title + " (" + Integer.toString(showData.year) + ") ";
                String overview = showData.overview;
                String airs = (showData.status == com.uwetrottmann.trakt5.enums.Status.RETURNING || showData.status == Status.IN_PRODUCTION)
                        ? showData.airs.day + " at " + Util.to12Hour(showData.airs.time) + " EST on " + showData.network : "Ended";
                String premier = new SimpleDateFormat("MMM dd, yyyy").format(new Date(showData.first_aired.toInstant().toEpochMilli()));
                String runtime = Integer.toString(showData.runtime);
                String country = showData.country + " - " + showData.language;
                String homepage = "<https://trakt.tv/shows/" + showData.ids.slug + ">\n<http://www.imdb.com/title/" + showData.ids.imdb + ">";

                try {
                    overview = guild.getChannelByID(Long.parseLong(bot.getDatabaseManager().getTV().getShow(showData.ids.imdb).getChannelID())).mention() + "\n" + overview;
                } catch (Exception ignored) {
                }

                EmbedBuilder embed = new EmbedBuilder();

                embed.withTitle(title);
                embed.withDescription(overview);
                embed.appendField("References:", homepage, false);
                embed.appendField("AIRS:", airs, true);
                embed.appendField("RUNTIME:", runtime, true);
                embed.appendField("PREMIERED:", premier, true);
                embed.appendField("COUNTRY:", country.toUpperCase(), true);
                embed.appendField("GENRES:", String.join(", ", showData.genres), true);
                embed.withColor(Util.getBotColor());

                Util.sendEmbed(event.getChannel(), embed.build());

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
    }

}
