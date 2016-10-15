package cback.commands;

import cback.ConfigManager;
import cback.TVBot;
import cback.Util;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMovieNight implements Command {
    @Override
    public String getName() {
        return "movienight";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        ConfigManager configManager = bot.getConfigManager();
        String text = message.getContent();
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID("226443478664609792"))) {
            Pattern pattern = Pattern.compile("^!movienight (.+) ?.+?");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String option = matcher.group(1);
                if (option.equalsIgnoreCase("set")) {
                    Pattern patternSet = Pattern.compile("!movienight set (.+) (.+)");
                    Matcher matcherSet = patternSet.matcher(text);
                    if (matcher.find()) {
                        String poll = "<https://goo.gl/forms/" + matcherSet.group(1) + ">";
                        String date = matcherSet.group(2);
                        if (date == null) {
                            date = "unspecified date";
                        }
                        Util.deleteMessage(client.getMessageByID(configManager.getConfigValue("mnID")));
                        String announcement = "Polls for next weeks movie night is now open! " + poll + " Movie night will be on " + date + ".";
                        IMessage thingy = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID), announcement);
                        String messageID = thingy.getID();
                        Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CHANNEL_ID), announcement);
                        try {
                            configManager.setConfigValue("poll", poll);
                            configManager.setConfigValue("date", date);
                            configManager.setConfigValue("mnID", messageID);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (option.equalsIgnoreCase("announce")) {
                    Pattern patternAnnounce = Pattern.compile("!movienight announce (.+)");
                    Matcher matcherAnnounce = patternAnnounce.matcher(text);
                    if (matcher.find()) {
                        String movie = matcherAnnounce.group(1);
                        String announcement = "The movie night poll is now closed! The movie that won is " + movie + ". Movie night will be on " + configManager.getConfigValue("date");
                        Util.deleteMessage(client.getMessageByID(configManager.getConfigValue("mnID")));
                        IMessage message2Delete = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID), announcement);
                        String messageID = message2Delete.getID();
                        Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CHANNEL_ID), announcement);
                        try {
                            configManager.setConfigValue("movie", movie);
                            configManager.setConfigValue("mnID", messageID);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (option.equalsIgnoreCase("start")) {
                    Pattern patternStart = Pattern.compile("!movienight start (.+)");
                    Matcher matcherStart = patternStart.matcher(text);
                    if (matcher.find()) {
                        String rabbit = "<https://rabb.it/" + matcherStart.group(1) + ">";
                        String announcement = "We are about to start watching " + configManager.getConfigValue("movie") + "! Come check it out here " + rabbit;
                        Util.deleteMessage(client.getMessageByID(configManager.getConfigValue("mnID")));
                        IMessage thingy = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID), announcement);
                        String messageID = thingy.getID();
                        Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CHANNEL_ID), announcement);
                        try {
                            configManager.setConfigValue("mnID", messageID);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
