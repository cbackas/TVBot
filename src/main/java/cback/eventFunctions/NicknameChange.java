package cback.eventFunctions;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.NicknameChangedEvent;
import sx.blah.discord.handle.obj.IGuild;

public class NicknameChange {

    @EventSubscriber
    public void nicknameChangeEvent(NicknameChangedEvent event) {
        IGuild guild = event.getGuild();
        if (guild.getStringID().equals("192441520178200577") || guild.getStringID().equals("256248900124540929")) {

            String oldName = event.getUser().getName();
            if (event.getOldNickname().isPresent()) {
                oldName = event.getOldNickname().get();
            }

            String newName = event.getUser().getDisplayName(guild);
            if (event.getNewNickname().isPresent()) {
                newName = event.getNewNickname().get();
            }

            //Sync nickname changes between servers
            if (guild.getStringID().equals("192441520178200577")) {
                try {
                    event.getClient().getGuildByID(256248900124540929l).setUserNickname(event.getUser(), newName);
                } catch (Exception ignored) {
                }
            } else if (guild.getStringID().equals("256248900124540929")) {
                try {
                    event.getClient().getGuildByID(192441520178200577l).setUserNickname(event.getUser(), newName);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
