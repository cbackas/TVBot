package cback.commands;

import cback.ConfigManager;
import cback.TVBot;
import cback.Util;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
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
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        ConfigManager configManager = bot.getConfigManager();
        IUser author = message.getAuthor();
        List<IRole> roles = author.getRolesForGuild(guild);
        String arguments = Arrays.stream(args).collect(Collectors.joining(" "));

        if (arguments.equalsIgnoreCase("ping")) {
            IMessage announcement = guild.getMessageByID(configManager.getConfigValue("mnID"));
            Util.sendPrivateMessage(message.getAuthor(), announcement.getContent());
            Util.deleteMessage(message);
        }

        if (roles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || roles.contains(guild.getRoleByID(TVBot.MOVIENIGHT_ROLE_ID))) {
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

                        Util.deleteMessage(client.getMessageByID(configManager.getConfigValue("mnID")));

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
                    Pattern patternAnnounce = Pattern.compile("^announce (.+)");
                    Matcher matcherAnnounce = patternAnnounce.matcher(arguments);
                    if (matcherAnnounce.find()) {
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
                    Pattern patternStart = Pattern.compile("^start (.+)");
                    Matcher matcherStart = patternStart.matcher(arguments);
                    if (matcherStart.find()) {
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
                Util.deleteMessage(message);
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
