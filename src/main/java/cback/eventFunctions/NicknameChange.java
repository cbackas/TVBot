package cback.eventFunctions;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.NickNameChangeEvent;
import sx.blah.discord.handle.obj.IGuild;

public class NicknameChange {

    @EventSubscriber
    public void nicknameChangeEvent(NickNameChangeEvent event) {
        IGuild guild = event.getGuild();
        if (guild.getID().equals("192441520178200577") || guild.getID().equals("256248900124540929")) {

            String oldName = event.getUser().getName();
            if (event.getOldNickname().isPresent()) {
                oldName = event.getOldNickname().get();
            }

            String newName = event.getUser().getDisplayName(guild);
            if (event.getNewNickname().isPresent()) {
                newName = event.getNewNickname().get();
            }

            //Sync nickname changes between servers
            if (guild.getID().equals("192441520178200577")) {
                try {
                    event.getClient().getGuildByID("256248900124540929").setUserNickname(event.getUser(), newName);
                } catch (Exception ignored) {
                }
            } else if (guild.getID().equals("256248900124540929")) {
                try {
                    event.getClient().getGuildByID("192441520178200577").setUserNickname(event.getUser(), newName);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
