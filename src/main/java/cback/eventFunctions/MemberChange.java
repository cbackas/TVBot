package cback.eventFunctions;

import cback.Channels;
import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;

import java.util.List;

public class MemberChange {
    private TVBot bot;

    public MemberChange(TVBot bot) {
        this.bot = bot;
    }

    public void memberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getId().equals(TVBot.getHomeGuild().getId())) {
            //Mute Check
            if (TVBot.getConfigManager().getConfigArray("muted").contains(event.getUser().getId())) {
                try {
                    event.getMember().getRoles().add(event.getGuild().getRoleById(231269949635559424L));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Join Counter
            int joined = Integer.parseInt(TVBot.getConfigManager().getConfigValue("joined"));
            TVBot.getConfigManager().setConfigValue("joined", String.valueOf(joined + 1));
        }
    }

    public void memberLeave(GuildMemberLeaveEvent event) {
        if (event.getGuild().getId().equals(TVBot.getHomeGuild().getId())) {
            User user = event.getUser();

            //Mute Check
            if (TVBot.getConfigManager().getConfigArray("muted").contains(event.getUser().getId())) {
                Util.sendMessage(Channels.SERVERLOG_CH_ID.getChannel(), user + " is muted and left the server. Their mute will be applied again when/if they return.");
            }

            //Leave Counter
            int left = Integer.parseInt(TVBot.getConfigManager().getConfigValue("left"));
            TVBot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));
        }
    }

    public void memberBanned(GuildBanEvent event) {
        if (event.getGuild().getId().equals(TVBot.getHomeGuild().getId())) {
            User user = event.getUser();

            //Reset xp
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getId());
            if (xp != null) {
                xp.setMessageCount(0);
                bot.getDatabaseManager().getXP().updateUserXP(xp);
            }

            //Mute Check
            if (TVBot.getConfigManager().getConfigArray("muted").contains(event.getUser().getId())) {
                List<String> mutedUsers = TVBot.getConfigManager().getConfigArray("muted");
                mutedUsers.remove(user.getId());
                TVBot.getConfigManager().setConfigValue("muted", mutedUsers);
            }

                //Leave Counter
                int left = Integer.parseInt(TVBot.getConfigManager().getConfigValue("left"));
                TVBot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));
        }
    }
}
