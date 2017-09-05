package cback.eventFunctions;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

public class MemberChange {
    private TVBot bot;

    public MemberChange(TVBot bot) {
        this.bot = bot;
    }

    @EventSubscriber
    public void memberJoin(UserJoinEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            if (event.getGuild().getStringID().equals("192441520178200577")) {
                //Mute Check
                if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getStringID())) {
                    try {
                        event.getUser().addRole(event.getGuild().getRoleByID(231269949635559424l));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //Join Counter
                int joined = Integer.parseInt(bot.getConfigManager().getConfigValue("joined"));
                bot.getConfigManager().setConfigValue("joined", String.valueOf(joined + 1));


            }
        }
    }

    @EventSubscriber
    public void memberLeave(UserLeaveEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            IUser user = event.getUser();

            if (event.getGuild().getStringID().equals("192441520178200577")) {
                //Mute Check
                if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getStringID())) {
                    Util.sendMessage(event.getGuild().getChannelByID(192444648545845248l), user + " is muted and left the server. Their mute will be applied again when/if they return.");
                }

                //Leave Counter
                int left = Integer.parseInt(bot.getConfigManager().getConfigValue("left"));
                bot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));

            }
        }
    }

    @EventSubscriber
    public void memberBanned(UserBanEvent event) {
        if (event.getGuild().getStringID().equals(TVBot.getHomeGuild().getStringID())) {
            IUser user = event.getUser();

            //Reset xp
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getStringID());
            if (xp != null) {
                xp.setMessageCount(0);
                bot.getDatabaseManager().getXP().updateUserXP(xp);
            }

            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getStringID())) {
                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                mutedUsers.remove(user.getStringID());
                bot.getConfigManager().setConfigValue("muted", mutedUsers);
            }

            if (event.getGuild().getStringID().equals("192441520178200577")) {
                //Leave Counter
                int left = Integer.parseInt(bot.getConfigManager().getConfigValue("left"));
                bot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));
            }
        }
    }
}
