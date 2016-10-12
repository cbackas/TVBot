package cback.serverfunctions;

import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.RequestBuffer;

import java.util.EnumSet;
import java.util.List;

public class MutePermissions {

    @EventSubscriber //Set all
    public void mutePermTrigger(MessageReceivedEvent event) {
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
    public void mutePermNewChannel(ChannelCreateEvent event) {
        IRole muted = event.getClient().getRoleByID("231269949635559424");
        try {
            event.getChannel().overrideRolePermissions(muted, EnumSet.noneOf(Permissions.class) ,EnumSet.of(Permissions.SEND_MESSAGES));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
