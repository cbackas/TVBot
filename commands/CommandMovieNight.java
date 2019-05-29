package cback.commands;

import cback.ConfigManager;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandMovieNight implements Command {
    @Override
    public String getName() {
        return "movienight";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("mn");
    }

    @Override
    public String getSyntax() {
        return "mn ping|set|announce|start";
    }

    @Override
    public String getDescription() {
        return "Does the movienight stuff. It's pretty complicated.";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.STAFF.id, TVRoles.MOVIENIGHT.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        ConfigManager configManager = bot.getConfigManager();
        String arguments = Arrays.stream(args).collect(Collectors.joining(" "));

        if (arguments.equalsIgnoreCase("ping")) {
            IMessage announcement = guild.getMessageByID(Long.parseLong(configManager.getConfigValue("mnID")));
            Util.sendPrivateMessage(message.getAuthor(), announcement.getContent());
            Util.deleteMessage(message);
        }

        Pattern patternOption = Pattern.compile("^(set|announce|start) (.+)");
        Matcher matcherOption = patternOption.matcher(arguments);
        if (matcherOption.find()) {
            String option = matcherOption.group(1);

            if (option.equalsIgnoreCase("set")) {
                Pattern patternSet = Pattern.compile("^set (\\w+) (.+)");
                Matcher matcherSet = patternSet.matcher(arguments);
                if (matcherSet.find()) {
                    String poll = "<https://goo.gl/forms/" + matcherSet.group(1) + ">";
                    String date = matcherSet.group(2);
                    String announcement = "The poll for the next movie night is now open! " + poll + " Movie night will be on " + date + ".";

                    Util.deleteMessage(client.getMessageByID(Long.parseLong(configManager.getConfigValue("mnID"))));

                    IMessage thingy = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CH_ID), announcement);
                    String messageID = thingy.getStringID();

                    Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CH_ID), announcement);
                    try {
                        configManager.setConfigValue("poll", poll);
                        configManager.setConfigValue("date", date);
                        configManager.setConfigValue("mnID", messageID);
                    } catch (Exception e) {
                        Util.reportHome(message, e);
                    }
                }

            } else if (option.equalsIgnoreCase("announce")) {
                Pattern patternAnnounce = Pattern.compile("^announce (.+)");
                Matcher matcherAnnounce = patternAnnounce.matcher(arguments);
                if (matcherAnnounce.find()) {
                    String movie = matcherAnnounce.group(1);
                    String announcement = "The movie night poll is now closed! The movie that won is " + movie + ". Movie night will be on " + configManager.getConfigValue("date");

                    Util.deleteMessage(client.getMessageByID(Long.parseLong(configManager.getConfigValue("mnID"))));

                    IMessage message2Delete = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CH_ID), announcement);
                    String messageID = message2Delete.getStringID();

                    Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CH_ID), announcement);
                    try {
                        configManager.setConfigValue("movie", movie);
                        configManager.setConfigValue("mnID", messageID);
                    } catch (Exception e) {
                        Util.reportHome(message, e);
                    }
                }

            } else if (option.equalsIgnoreCase("start")) {
                Pattern patternStart = Pattern.compile("^start (.+)");
                Matcher matcherStart = patternStart.matcher(arguments);
                if (matcherStart.find()) {
                    String rabbit = "<https://rabb.it/" + matcherStart.group(1) + ">";
                    String announcement = "We are about to start watching " + configManager.getConfigValue("movie") + "! Come check it out here " + rabbit;

                    Util.deleteMessage(client.getMessageByID(Long.parseLong(configManager.getConfigValue("mnID"))));

                    IMessage thingy = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CH_ID), announcement);
                    String messageID = thingy.getStringID();

                    Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CH_ID), announcement);
                    try {
                        configManager.setConfigValue("mnID", messageID);
                    } catch (Exception e) {
                        Util.reportHome(message, e);
                    }
                }
            }
            Util.deleteMessage(message);
        }
    }

}
