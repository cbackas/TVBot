package cback;

import cback.commands.CommandSort;
import cback.database.tv.Airing;
import cback.database.tv.Show;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.VerificationLevel;
import sx.blah.discord.util.EmbedBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
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

            processNewAirings(currentTime);
            processOldAirings(currentTime);

        }, airingCheckWaitTime, CHECK_AIRING_INTERVAL, TimeUnit.SECONDS);

        exec.scheduleAtFixedRate(() -> {

            //delete old airings from database
            pruneDeletedAirings();
            //update database with new airings for next 3 days
            bot.getTraktManager().updateAiringData();

        }, 0, DAILY_INTERVAL, TimeUnit.SECONDS);

        //midnight
        //todo make this work with daylight savings bologna
        int currentTimeEST = time - 18000; //EST time, subtract 5 hours from UTC
        int midnightWaitTime = roundUp(currentTimeEST, DAILY_INTERVAL) - currentTimeEST + 45; //seconds until midnight
        exec.scheduleAtFixedRate(() -> {

            System.out.println("Midnight processed.");
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
                boolean isPermChannel = CommandSort.getPermChannels(bot.getClient().getGuildByID(TVBot.HOMESERVER_GLD_ID)).contains(bot.getClient().getChannelByID(Long.parseLong(show.getChannelID())).getCategory());
                if (show != null && !isPermChannel) {
                    TraktManager tm = bot.getTraktManager();
                    String network = tm.showSummary(show.getShowID()).network;

                    IChannel announceChannel = bot.getClient().getChannelByID(TVBot.NEWEPISODE_CH_ID);
                    IChannel globalChannel = bot.getClient().getChannelByID(TVBot.GENERAL_CH_ID);
                    IChannel showChannel = bot.getClient().getChannelByID(Long.parseLong(show.getChannelID()));

                    String message = "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start on " + network + ". Go to " + showChannel.mention() + " for live episode discussion!";

                    if (network.equalsIgnoreCase("netflix") || network.equalsIgnoreCase("amazon")) {
                        if (bulkShowIDs.contains(show.getShowID())) {
                            bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
                            return;
                        } else {
                            Pattern pattern = Pattern.compile("^S([0-9]+)E.+");
                            Matcher matcher = pattern.matcher(airing.getEpisodeInfo());
                            String season = matcher.group(1);
                            message = "**" + show.getShowName() + " season " + season + " is about to be released on " + network + ". Go to " + showChannel.mention() + " to see not so live episode discussion!";
                            bulkShowIDs.add(show.getShowID());
                        }
                    }

                    IMessage announceMessage = Util.sendBufferedMessage(announceChannel, message);
                    airing.setMessageID(announceMessage.getStringID());
                    bot.getDatabaseManager().getTV().updateAiringMessage(airing);
                    Util.sendBufferedMessage(globalChannel, message);
                    Util.sendBufferedMessage(showChannel, "**" + show.getShowName() + " " + airing.getEpisodeInfo() + "** is about to start.");

                    System.out.println("Sent announcement for " + airing.getEpisodeInfo());
                    //Delete airing once it's done
                    bot.getDatabaseManager().getTV().deleteAiring(airing.getEpisodeID());
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
    public void processOldAirings(int currentTime) {
        //Old delete announcement stuff
        /*//get last 30 shows alerted
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
        */

        if (bot.toggleState("autosecure")) {
            //Set security level if a show is airing
            List<Airing> oldAirings = bot.getDatabaseManager().getTV().getOldAirings();
            //if episode aired over 2 hours ago, delete message from announcements channel
            long aCount = oldAirings.stream().filter(airing -> currentTime - airing.getAiringTime() >= DELETE_THRESHOLD).count();
            if (aCount >= 1) {
                Util.setSecurity(VerificationLevel.MEDIUM);
            } else {
                Util.setSecurity(VerificationLevel.HIGH);
            }
        }
    }

    /***
     * Delete airing entries from the database if episode aired over a week ago
     */
    public void pruneDeletedAirings() {
        //delete week old airings whose announcements were deleted
        List<Airing> deletedAirings = bot.getDatabaseManager().getTV().getDeletedAirings();
        deletedAirings.stream().filter(airing -> Util.getCurrentTime() - airing.getAiringTime() >= 86400).forEach(airing -> {
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
        LocalDate now = LocalDate.now();
        String dayOfWeek = now.getDayOfWeek().name();
        String month = now.getMonth().name();
        int day = now.getDayOfMonth();

        List<Airing> nextAirings = bot.getDatabaseManager().getTV().getNewAirings();

        //if it airs in next 24 hours it goes on the count
        int count = 0;
        for(Airing a : nextAirings) {
            if ((a.getAiringTime() - Util.getCurrentTime() <= DAILY_INTERVAL) && (a.getAiringTime() - Util.getCurrentTime() > 0)) {
                Show show = bot.getDatabaseManager().getTV().getShow(a.getShowID());
                if (show != null) {
                    count++;
                }
            }
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed
                .withTitle(dayOfWeek + ", " + month + " " + day)
                .withColor(Util.BOT_COLOR);

        if (count > 1) {
            embed.withDesc("There are " + count + " episodes airing today! Stick around to see what they are.");
        } else if (count == 1) {
            embed.withDesc("There is " + count + " episode airing today! Stick around to see what it is.");
        } else {
            embed.withDesc("There aren't any new episodes airing today. Maybe tomorrow will be interesting.");
        }

        Util.sendEmbed(bot.getClient().getChannelByID(TVBot.NEWEPISODE_CH_ID), embed.build());
    }
}