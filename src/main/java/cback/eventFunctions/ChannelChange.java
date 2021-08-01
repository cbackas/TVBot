package cback.eventFunctions;

import cback.TVBot;
import cback.TraktManager;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class ChannelChange extends ListenerAdapter {
    private TVBot bot;

    public ChannelChange(TVBot bot) {
        this.bot = bot;
    }

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
                MessageEmbed matchEmbed = new EmbedBuilder().setColor(Util.getBotColor())
                        .setDescription("Found possible match: **" + showData.title + "**." +
                                "\n " + "<http://www.imdb.com/title/" + showData.ids.imdb + ">")
                        .setFooter("/show add " + showData.ids.imdb)
                        .build();
                Util.sendEmbed(event.getChannel(), matchEmbed);
            }
        }
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        // delete database entries related to a chennel when the channel is deleted
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            List<cback.database.tv.Show> shows = bot.getDatabaseManager().getTV().getShowsByChannel(event.getChannel().getId());
            if (shows != null) {
                shows.forEach(show -> {
                    if (bot.getDatabaseManager().getTV().deleteShow(show.getShowID()) > 0) {
                        Util.getLogger().info("Channel deleted (" + event.getChannel().getName() + ") and " + show.getShowName() + " removed from database");
                    }
                });
            }
        }
    }
}