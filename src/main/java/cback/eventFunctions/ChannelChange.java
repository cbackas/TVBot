package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

public class ChannelChange extends ListenerAdapter {
    private TVBot bot;

    public ChannelChange(TVBot bot) {
        this.bot = bot;
    }

    //Set all
    public void onMessageReceived(MessageReceivedEvent event) {
        //TODO idk wtf
//        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
//            Message message = event.getMessage();
//            String text = message.getContentRaw();
//            JDA client = event.getJDA();
//            if (text.equalsIgnoreCase("!setmuteperm") && message.getAuthor().getId().equals("73416411443113984")) {
//                List<Channel> channelList = client.getGuildById(192441520178200577L).getChannels();
//                Guild guild = event.getJDA().getGuildById(192441520178200577L);
//                Role muted = event.getGuild().getRoleById(239233306325942272L);
//
//                for (Channel channels : channelList) {
//                    try {
//                        channels.createPermissionOverride(muted).setDeny(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES).queue();
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                System.out.println("Set muted role");
//                Util.deleteMessage(message);
//            }
//        }
    }

    //New Channel
    @Override
    public void onTextChannelCreate(TextChannelCreateEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            //TODO seems like another bot does this already?

            //Set muted role
//            Guild guild = event.getJDA().getGuildById(192441520178200577L);
//            Role muted = guild.getRoleById(231269949635559424L);
//            try {
//                event.getChannel().createPermissionOverride(muted).setDeny(Permission.MESSAGE_WRITE).queue();
//            } catch (Exception e) {
//                Util.reportHome(e);
//            }

            //Check for tv show
            TraktManager trakt = bot.getTraktManager();
            String showName = event.getChannel().getName().replace("-", " ");
            Show showData = trakt.showSummaryFromName(showName);
            if (showData != null) {
                Util.simpleEmbed(event.getChannel(),
                        "Found possible show: **" +
                                showData.title +
                                "**. The showID is: " +
                                showData.ids.imdb + "." +
                                "\n " +
                                "<http://www.imdb.com/title/" + showData.ids.imdb + ">" +
                                "\nAdmins use: !addshow " + showData.ids.imdb + " here to add the show");
            }
        }
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            List<cback.database.tv.Show> shows = bot.getDatabaseManager().getTV().getShowsByChannel(event.getChannel().getId());
            if (shows != null) {
                shows.forEach(show -> {
                    if (bot.getDatabaseManager().getTV().deleteShow(show.getShowID()) > 0) {
                        String message = "Channel Deleted: Removed show " + show.getShowName() + " from database automatically.";
                        System.out.println(message);
                    }
                });
            }
        }
    }
}