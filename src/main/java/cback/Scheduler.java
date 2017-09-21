package cback;

import cback.database.tv.Airing;
import cback.database.tv.Show;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            //processOldAirings(currentTime);

        }, airingCheckWaitTime, CHECK_AIRING_INTERVAL, TimeUnit.SECONDS);

        exec.scheduleAtFixedRate(() -> {

            //delete old airings from database
            pruneDeletedAirings();
            //update database with new airings for next 3 days
            bot.getTraktManager().updateAiringData();

        }, 0, DAILY_INTERVAL, TimeUnit.SECONDS);

        //update user count at midnight every night
        int currentTimeEST = time - 14400; //EST time, subtract 4 hours from UTC
        int midnightWaitTime = roundUp(currentTimeEST, DAILY_INTERVAL) - currentTimeEST + 45; //seconds until midnight
        exec.scheduleAtFixedRate(() -> {

            /**
             * todo : message at midnight in the new episodes channel saying date and how many shows are airing that day
             */
            updateUserCount();
            resetUserChange();
            sendDailyMessage();

        }, midnightWaitTime, DAILY_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Send messages for shows that are about to air and set database values accordingly
     */
    public void processNewAirings(int currentTime) {
        //get next 30 shows airing
        List<Airing> nextAirings = bot.getDatabaseManager().getTV().getNewAirings();
        List<String> bulkShowIDs = new ArrayList<>();
        //if it airs in next 11 minutes, send message and update in database
        nextAirings.stream().filter(airing -> airing.getAiringTime() - currentTime <= ALERT_TIME_THRESHOLD).forEach(airing -> {
            try {
                Show show = bot.getDatabaseManager().getTV().getShow(airing.getShowID());
                if (show != null) {
                    TraktManager tm = bot.getTraktManager();
                    String network = tm.showSummary(show.getShowID()).network;

                    IChannel announceChannel = bot.getClient().getChannelByID(TVBot.NEWEPISODE_CH_ID);
                    IChannel globalChannel = bot.getClient().getChannelByID(TVBot.GENERAL_CH_ID);
                    IChannel showChannel = bot.getClient().getChannelByID(Long.parseLong(show.getChannelID()));

                    String message = "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start on " + network + ". Go to " + showChannel.mention() + " for live episode discussion!";

                    if (network.equalsIgnoreCase("netflix") || network.equalsIgnoreCase("amazon")) {
                        if (bulkShowIDs.contains(show.getShowID())) {
                            return;
                        } else {
                            Pattern pattern = Pattern.compile("^S([0-9]+)E[0-9]+");
                            Matcher matcher = pattern.matcher(airing.getEpisodeInfo());
                            message = "**" + show.getShowName() + " season " + matcher.group(1) + " is about to be released on " + network + ". Go to " + showChannel.mention() + " to see not so live episode discussion!";;
                            bulkShowIDs.add(show.getShowID());
                        }
                    }

                    IMessage announceMessage = Util.sendBufferedMessage(announceChannel, message);
                    airing.setMessageID(announceMessage.getStringID());
                    bot.getDatabaseManager().getTV().updateAiringMessage(airing);
                    Util.sendBufferedMessage(globalChannel, message);
                    Util.sendBufferedMessage(showChannel, "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start.");

                    System.out.println("Sent announcement for " + airing.getEpisodeInfo());
                } else {
                    System.out.println("Tried to announce airing for unsaved show, deleting airing...");
                    bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
                }

            } catch (Exception e) {
            }
        });
    }

    /**
     * Delete messages for episodes that have finished airing and set database values accordingly
     */
    /*public void processOldAirings(int currentTime) {
        //get last 30 shows alerted
        List<Airing> oldAirings = bot.getDatabaseManager().getTV().getOldAirings();
        //if episode aired over 2 hours ago, delete message from announcements channel
        oldAirings.stream().filter(airing -> currentTime - airing.getAiringTime() >= DELETE_THRESHOLD).forEach(airing -> {
            try {

                IMessage toDelete = bot.getClient().getChannelByID(TVBot.ANNOUNCEMENT_CH_ID).getMessageByID(airing.getMessageID());
                if(toDelete == null)
                    throw new NullPointerException("Message to delete was not found!");
                Util.deleteBufferedMessage(toDelete);

                System.out.println("Deleted announcement message for " + airing.getEpisodeInfo());

            } catch (Exception e) {
                System.out.println("Error deleting announcement message for " + airing.getEpisodeInfo());
                e.printStackTrace();
            } finally {
                airing.setMessageID("DELETED");
                bot.getDatabaseManager().getTV().updateAiringMessage(airing);
            }
        });
    }*/

    /***
     * Delete airing entries from the database if episode aired over a week ago
     */
    public void pruneDeletedAirings() {
        //delete week old airings whose announcements were deleted
        List<Airing> deletedAirings = bot.getDatabaseManager().getTV().getDeletedAirings();
        deletedAirings.stream().filter(airing -> Util.getCurrentTime() - airing.getAiringTime() >= 604800).forEach(airing -> {
            bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
        });
    }

    /**
     * Update the number of Lounge server members in the config
     */
    public void updateUserCount() {
        IGuild loungeGuild = bot.getClient().getGuildByID(192441520178200577l);
        if (loungeGuild != null) {
            bot.getConfigManager().setConfigValue("userCount", String.valueOf(loungeGuild.getUsers().size()));
        }
    }

    /**
     * Reset daily user change
     */
    public void resetUserChange() {
        bot.getConfigManager().setConfigValue("left", "0");
        bot.getConfigManager().setConfigValue("joined", "0");
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

    /**
     * Sends a message in the new show channel daily to separate days
     */
    public void sendDailyMessage() {
        EmbedBuilder embed = new EmbedBuilder();

        LocalDate now = LocalDate.now();
        String dayOfWeek = now.getDayOfWeek().name();
        String month = now.getMonth().name();
        int day = now.getDayOfMonth();

        List<Airing> nextAirings = bot.getDatabaseManager().getTV().getNewAirings();
        //if it airs in next 24 hours it goes on the count
        long count = nextAirings.stream().filter(airing -> airing.getAiringTime() - Util.getCurrentTime() <= DAILY_INTERVAL).count();

        embed
                .withTitle(dayOfWeek + ", " + month + " " + day)
                .withColor(Util.BOT_COLOR);

        if (count > 0) {
            embed.withDesc("There are " + count + " episodes airing today! Stick around to see what they are.");
        } else {
            embed.withDesc("There aren't any new episodes airing today. Maybe tomorrow will be interesting.");
        }

        Util.sendEmbed(bot.getClient().getChannelByID(TVBot.DEV_CH_ID), embed.build());
    }
}