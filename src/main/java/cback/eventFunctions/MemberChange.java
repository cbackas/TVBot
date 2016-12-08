package cback.eventFunctions;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import sun.awt.image.URLImageSource;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.UserBanEvent;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

import java.net.URL;
import java.util.List;

public class MemberChange {
    private TVBot bot;

    public MemberChange(TVBot bot) {
        this.bot = bot;
    }

    @EventSubscriber
    public void memberJoin(UserJoinEvent event) {
        if (event.getGuild().getID().equals("192441520178200577")) {
            IUser user = event.getUser();

            //Memberlog message
            Util.sendMessage(bot.getClient().getChannelByID(TVBot.MEMBERLOG_CHANNEL_ID), "\uD83D\uDCE5  " + user.getName() + " **joined** the server. " + user.mention());

            //Bot Check
            if (event.getUser().isBot()) {
                Util.sendMessage(bot.getClient().getChannelByID(TVBot.BOTLOG_CHANNEL_ID), "**A bot has joined the server - **" + user.mention());
            }

            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getID())) {
                try {
                    event.getUser().addRole(event.getGuild().getRoleByID("231269949635559424"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Join Counter
            int joined = Integer.parseInt(bot.getConfigManager().getConfigValue("joined"));
            bot.getConfigManager().setConfigValue("joined", String.valueOf(joined + 1));

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
    }

    @EventSubscriber
    public void memberLeave(UserLeaveEvent event) {
        if (event.getGuild().getID().equals("192441520178200577")) {
            IUser user = event.getUser();

            //Memberlog message
            Util.sendMessage(bot.getClient().getChannelByID(TVBot.MEMBERLOG_CHANNEL_ID), "\uD83D\uDCE4  " + user.getName() + " **left** the server. " + user.mention());

            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getID())) {
                Util.sendMessage(event.getGuild().getChannelByID("192444648545845248"), user + " is muted and left the server. Their mute will be applied again when/if they return.");
            }

            //Leave Counter
            int left = Integer.parseInt(bot.getConfigManager().getConfigValue("left"));
            bot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));

        }
    }

    @EventSubscriber
    public void memberBanned(UserBanEvent event) {
        if (event.getGuild().getID().equals("192441520178200577")) {
            IUser user = event.getUser();

            //Memberlog message
            Util.sendMessage(bot.getClient().getChannelByID(TVBot.MEMBERLOG_CHANNEL_ID), "\uD83D\uDD28  " + user.getName() + " was **banned**. " + user.mention());

            //Mute Check
            if (bot.getConfigManager().getConfigArray("muted").contains(event.getUser().getID())) {
                List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                mutedUsers.remove(user.getID());
                bot.getConfigManager().setConfigValue("muted", mutedUsers);
            }

            //Leave Counter
            int left = Integer.parseInt(bot.getConfigManager().getConfigValue("left"));
            bot.getConfigManager().setConfigValue("left", String.valueOf(left + 1));

            //Reset xp
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getID());
            if (xp != null) {
                xp.setMessageCount(0);
                bot.getDatabaseManager().getXP().updateUserXP(xp);
                Util.sendMessage(bot.getClient().getChannelByID(TVBot.MEMBERLOG_CHANNEL_ID), "Reset xp for " + user.getDisplayName(event.getGuild()));
            }
        }
    }
}
