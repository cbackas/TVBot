package cback.eventFunctions;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;

public class MemberChange {
    String logChannel = "217450005462646794";
    private TVBot bot;

    public MemberChange(TVBot bot) {
        this.bot = bot;
    }

    @EventSubscriber
    public void memberJoin(UserJoinEvent event) {
        IGuild server = event.getGuild();
        String user = event.getUser().mention();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user + " **joined** the server.");
        if (event.getUser().isBot()) {
            Util.sendMessage(server.getChannelByID(TVBot.BOTLOG_CHANNEL_ID), "A bot has joined the server - " + user);
        }
        if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getID())) {
            try {
                event.getUser().addRole(event.getGuild().getRoleByID("231269949635559424"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventSubscriber
    public void memberLeave(UserLeaveEvent event) {
        String user = event.getUser().mention();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user + " **left** the server.");
    }
}
