package cback.eventFunctions;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.NickNameChangeEvent;
import sx.blah.discord.handle.obj.IGuild;

public class NicknameChange {

    @EventSubscriber
    public void nicknameChangeEvent(NickNameChangeEvent event) {
        IGuild guild = event.getGuild();
        if (guild.getID().equals("192441520178200577")) {

            String oldName = event.getUser().getName();
            if (event.getOldNickname().isPresent()) {
                oldName = event.getOldNickname().get();
            }

            String newName = event.getUser().getDisplayName(guild);
            if (event.getNewNickname().isPresent()) {
                newName = event.getNewNickname().get();
            }

            Util.sendMessage(guild.getChannelByID(TVBot.MEMBERLOG_CHANNEL_ID), event.getUser().mention() + "'s nickname changed from ``" + oldName + "`` to ``" + newName + "``");
        }
    }
}
