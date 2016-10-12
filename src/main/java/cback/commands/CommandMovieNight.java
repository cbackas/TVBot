package cback.commands;

import cback.ConfigManager;
import cback.TVBot;
import cback.Util;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;

public class CommandMovieNight implements Command {
    @Override
    public String getName() {
        return "movienight";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        ConfigManager configManager = bot.getConfigManager();
        String option = args[0];
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID("226443478664609792"))) {
            if (option.equalsIgnoreCase("set")) {
                String poll = "<https://goo.gl/forms/" + args[1] + ">";
                String date = StringUtils.join(" ", Arrays.copyOfRange(args, 2, args.length));
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
            } else if (option.equalsIgnoreCase("announce")) {
                String movie = StringUtils.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String announcement = "The movie night poll is now closed! The movie that won is " + movie + ". Movie night will be on " + configManager.getConfigValue("date");
                Util.deleteMessage(client.getMessageByID(configManager.getConfigValue("mnID")));
                IMessage thingy = Util.sendBufferedMessage(guild.getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID), announcement);
                String messageID = thingy.getID();
                Util.sendBufferedMessage(guild.getChannelByID(TVBot.GENERAL_CHANNEL_ID), announcement);
                try {
                    configManager.setConfigValue("movie", movie);
                    configManager.setConfigValue("mnID", messageID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (option.equalsIgnoreCase("start")) {
                String rabbit = "<https://rabb.it/" + args[1] + ">";
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

    @Override
    public boolean isLogged() {
        return true;
    }
}
