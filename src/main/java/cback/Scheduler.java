package cback;

import cback.database.Airing;
import cback.database.Show;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    /**
     * Check for airings (and delete messages from old airings) every X seconds
     */
    private static final int CHECK_AIRING_INTERVAL = 300; //5 minutes
    /**
     * Number of seconds in one day
     */
    private static final int DAILY_INTERVAL = 86400; //24 hours
    /**
     * Send alert if show airs within X seconds from time of checking
     */
    private static final int ALERT_TIME_THRESHOLD = 660; //11 minutes
    /**
     * Delete message from announcements channel if show aired over X seconds from time of checking
     */
    private static final int DELETE_THRESHOLD = 7000; //~2 hours

    private TVBot bot;

    public Scheduler(TVBot bot) {
        this.bot = bot;
        onInit();
    }

    private void onInit() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        int time = Util.getCurrentTime(); //current epoch time in seconds

        int airingCheckWaitTime = roundUp(time, CHECK_AIRING_INTERVAL) - time; //seconds until next 5 minute interval
        exec.scheduleAtFixedRate(() -> {

            //establish current time (will be 5 min interval)
            int currentTime = Util.getCurrentTime();
            //announce new airings
            processNewAirings(currentTime);
            //delete messages for old airings
            processOldAirings(currentTime);

        }, airingCheckWaitTime, CHECK_AIRING_INTERVAL, TimeUnit.SECONDS);

        exec.scheduleAtFixedRate(() -> {

            //delete old airings from database
            pruneDeletedAirings();
            //update database with new airings for next 3 days
            bot.getTraktManager().updateAiringData();

        }, 0, DAILY_INTERVAL, TimeUnit.SECONDS);

        //update user count at midnight every night
        int currentTimeEST = time - 14400; //EST time, subtract 4 hours from UTC
        int midnightWaitTime = roundUp(currentTimeEST, DAILY_INTERVAL) - currentTimeEST; //seconds until midnight
        exec.scheduleAtFixedRate(() -> {

            updateUserCount();

        }, midnightWaitTime, DAILY_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Send messages for shows that are about to air and set database values accordingly
     */
    public void processNewAirings(int currentTime) {
        //get next 30 shows airing
        List<Airing> nextAirings = bot.getDatabaseManager().getNewAirings();
        //if it airs in next 11 minutes, send message and update in database
        nextAirings.stream().filter(airing -> airing.getAiringTime() - currentTime <= ALERT_TIME_THRESHOLD).forEach(airing -> {
            try {
                Show show = bot.getDatabaseManager().getShow(airing.getShowID());
                if (show != null) {
                    IChannel announceChannel = bot.getClient().getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID);
                    IChannel globalChannel = bot.getClient().getChannelByID(TVBot.GENERAL_CHANNEL_ID);
                    IChannel showChannel = bot.getClient().getChannelByID(show.getChannelID());

                    String message = "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start. Go to " + showChannel.mention() + " for live episode discussion!";

                    IMessage announceMessage = Util.sendBufferedMessage(announceChannel, message);
                    airing.setMessageID(announceMessage.getID());
                    bot.getDatabaseManager().updateAiringMessage(airing);
                    Util.sendBufferedMessage(globalChannel, message);
                    Util.sendBufferedMessage(showChannel, "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start.");

                    System.out.println("Sent announcement for " + airing.getEpisodeInfo());
                } else {
                    System.out.println("Tried to announce airing for unsaved show, deleting airing...");
                    bot.getDatabaseManager().deleteAiring(airing.getEpisodeID());
                }

            } catch (Exception e) {
            }
        });
    }

    /**
     * Delete messages for episodes that have finished airing and set database values accordingly
     */
    public void processOldAirings(int currentTime) {
        //get last 30 shows alerted
        List<Airing> oldAirings = bot.getDatabaseManager().getOldAirings();
        //if episode aired over 2 hours ago, delete message from announcements channel
        oldAirings.stream().filter(airing -> currentTime - airing.getAiringTime() >= DELETE_THRESHOLD).forEach(airing -> {
            try {
                bot.getClient().getMessageByID(airing.getMessageID()).delete();
                airing.setMessageID("DELETED");
                bot.getDatabaseManager().updateAiringMessage(airing);

                System.out.println("Deleted announcement message for " + airing.getEpisodeInfo());
            } catch (Exception e) {
                System.out.println("Error deleting announcement for " + airing.getEpisodeInfo());
            }
        });
    }

    /***
     * Delete airing entries from the database if episode aired over a week ago
     */
    public void pruneDeletedAirings() {
        //delete week old airings whose announcements were deleted
        List<Airing> deletedAirings = bot.getDatabaseManager().getDeletedAirings();
        deletedAirings.stream().filter(airing -> Util.getCurrentTime() - airing.getAiringTime() >= 604800).forEach(airing -> {
            bot.getDatabaseManager().deleteAiring(airing.getEpisodeID());
        });
    }

    /**
     * Update the number of Lounge server members in the config
     */
    public void updateUserCount() {
        IGuild loungeGuild = bot.getClient().getGuildByID("192441520178200577");
        if (loungeGuild != null) {
            bot.getConfigManager().setConfigValue("userCount", String.valueOf(loungeGuild.getUsers().size()));
        }
    }

    /**
     * Rounds i to next number divisible by v
     *
     * @param i
     * @param v
     * @return the rounded number divisible by v
     */
    public static int roundUp(double i, int v) {
        Double rounded = Math.ceil(i / v) * v;
        return rounded.intValue();
    }
}