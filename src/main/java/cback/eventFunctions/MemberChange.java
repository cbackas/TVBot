package cback.eventFunctions;

import cback.TVBot;
import cback.Util;
import sun.awt.image.URLImageSource;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

import java.net.URL;

public class MemberChange {
    private String logChannel = "217450005462646794";
    private TVBot bot;

    public MemberChange(TVBot bot) {
        this.bot = bot;
    }

    @EventSubscriber
    public void memberJoin(UserJoinEvent event) {
        IGuild server = event.getGuild();
        IUser user = event.getUser();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user.getName() + " joined the server. " + user.mention());

        //Bot Check
        if (event.getUser().isBot()) {
            Util.sendMessage(server.getChannelByID(TVBot.BOTLOG_CHANNEL_ID), "A bot has joined the server - " + user.mention());
        }

        //Mute Check
        if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getID())) {
            try {
                event.getUser().addRole(event.getGuild().getRoleByID("231269949635559424"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        //PM on join
        try {
            Util.sendPrivateMessage(user, "Welcome to The Lounge! We primarily discuss television but we also discuss other things on occasion. We are an English server.\n" +
                    "\n" +
                    "``Rules``\n" +
                    "**Rule 1:** Stay Civil\n" +
                    "\n" +
                    "**Rule 2:** No Spam\n" +
                    "\n" +
                    "**Rule 3:** No Self-Promotion\n" +
                    "\n" +
                    "**Rule 4:** Keep spoilers in their respective channels.\n" +
                    "\n" +
                    "**Rule 5:** No NSFW of any kind.\n" +
                    "\n" +
                    "**Rule 6:** Do not abuse or add bots.\n" +
                    "\n" +
                    "**Other:** Our rules are subject to change, more in depth rules are on the server, #announcements.\n" +
                    "\n" +
                    "``Important info``\n" +
                    "We are constantly adding new channels and deleting inactive ones, If you don't see your favorite show head over to #suggestions and simply do !suggest 'your show'. It will probably be added soon!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventSubscriber
    public void memberLeave(UserLeaveEvent event) {
        IUser user = event.getUser();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user.getName() + " left the server. " + user.mention());
        if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getID())) {
            Util.sendMessage(event.getGuild().getChannelByID("192444648545845248"), user + " is muted and left the server. Their mute will be applied again when/if they return.");
        }
    }
}
