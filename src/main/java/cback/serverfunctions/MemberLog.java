package cback.serverfunctions;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;

public class MemberLog {
    String logChannel = TVBot.LOG_CHANNEL_ID;

    @EventSubscriber
    public void memberJoin(UserJoinEvent event) {
        IGuild server = event.getGuild();
        String user = event.getUser().mention();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user + " **joined** the server.");
    }

    @EventSubscriber
    public void memberLeave(UserLeaveEvent event) {
        String user = event.getUser().mention();
        Util.sendMessage(event.getClient().getChannelByID(logChannel), user + " **left** the server.");
    }
}
