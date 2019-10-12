package cback.eventFunctions;

import cback.Channels;
import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class MemberChange extends ListenerAdapter {
    private TVBot bot;

    public MemberChange(TVBot bot) {
        this.bot = bot;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getId())) {
                try {
                    event.getMember().getRoles().add(event.getGuild().getRoleById(231269949635559424L));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Join Counter
            int joined = Integer.parseInt(bot.getConfigManager().getConfigValue("joined"));
            bot.getConfigManager().setConfigValue("joined", String.valueOf(joined + 1));
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            User user = event.getUser();

            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getId())) {
                Util.sendMessage(Channels.SERVERLOG_CH_ID.getChannel(), user + " is muted and left the server. Their mute will be applied again when/if they return.");
            }

            //Leave Counter
            int left = Integer.parseInt(bot.getConfigManager().getConfigValue("left"));
            bot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));
        }
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        if (event.getGuild().getId().equals(bot.getHomeGuild().getId())) {
            User user = event.getUser();

            //Reset xp
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getId());
            if (xp != null) {
                xp.setMessageCount(0);
                bot.getDatabaseManager().getXP().updateUserXP(xp);
            }

            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getId())) {
                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                mutedUsers.remove(user.getId());
                bot.getConfigManager().setConfigValue("muted", mutedUsers);
            }

                //Leave Counter
                int left = Integer.parseInt(bot.getConfigManager().getConfigValue("left"));
                bot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));
        }
    }
}
