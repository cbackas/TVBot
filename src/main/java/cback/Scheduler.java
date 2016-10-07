package cback;

import cback.database.Airing;
import cback.database.Show;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    /**
     * Check for airings every X seconds
     */
    private static final int WAIT_INTERVAL = 300; //5 minutes
    /**
     * Send alert if show airs X seconds from now when checking
     */
    private static final int ALERT_TIME_THRESHOLD = 660; //11 minutes
    /**
     * Delete message from announcements if show aired X seconds ago
     */
    private static final int DELETE_THRESHOLD = 7000; //~2 hours
    /**
     * Update airing data every X seconds AND prune week old airings from the database
     */
    private static final int UPDATE_AIRING_INTERVAL = 86400; //24 hours

    private TVBot bot;

    public Scheduler(TVBot bot) {
        this.bot = bot;
        onInit();
    }

    private void onInit() {
        int time = Util.toInt(System.currentTimeMillis() / 1000);
        int next5MinDivisible = roundUp(time, WAIT_INTERVAL);
        int timeToWait = next5MinDivisible - time;
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {

            //establish current time (will be 5 min interval)
            int currentTime = Util.getCurrentTime();

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
                        System.out.println("Tried to announce airing for nonsaved show, deleting airing...");
                        bot.getDatabaseManager().deleteAiring(airing.getEpisodeID());
                    }

                } catch (Exception e) {
                }
            });


            //get last 30 shows alerted
            List<Airing> oldAirings = bot.getDatabaseManager().getOldAirings();
            //if episode aired over 2 hours ago, delete message from announcements
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

        }, timeToWait, WAIT_INTERVAL, TimeUnit.SECONDS);

        exec.scheduleAtFixedRate(() -> {

            //delete week old airings whose announcements were deleted
            List<Airing> deletedAirings = bot.getDatabaseManager().getDeletedAirings();
            deletedAirings.stream().filter(airing -> Util.getCurrentTime() - airing.getAiringTime() >= 604800).forEach(airing -> {
                bot.getDatabaseManager().deleteAiring(airing.getEpisodeID());
            });

            //update database with new airings for next 3 days
            bot.getTraktManager().updateAiringData();

        }, 0, UPDATE_AIRING_INTERVAL, TimeUnit.SECONDS);
    }

    public static int roundUp(double i, int v) {
        Double rounded = Math.ceil(i / v) * v;
        return rounded.intValue();
    }
}