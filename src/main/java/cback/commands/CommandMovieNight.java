package cback.commands;

import cback.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandMovieNight extends Command {

    private TVBot bot;

    public CommandMovieNight() {
        this.bot = TVBot.getInstance();
        this.name = "movienight";
        this.aliases = new String[]{"mn"};
        this.arguments = "mn ping|set|announce|start";
        this.help = "Does the movienight stuff. It's pretty complicated";
        this.requiredRole = TVRoles.STAFF.name;
        this.requiredRole = TVRoles.MOVIENIGHT.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);
        ConfigManager configManager = bot.getConfigManager();
        String arguments = String.join(" ", args);

        if(arguments.equalsIgnoreCase("ping")) {
            RestAction<Message> announcement = commandEvent.getGuild().getTextChannelById(commandEvent.getTextChannel().getIdLong()).getMessageById(Long.parseLong(configManager.getConfigValue("mnID")));
            Util.sendPrivateMessage(commandEvent.getAuthor(), announcement.toString());
            Util.deleteMessage(commandEvent.getMessage());
        }

        Pattern patternOption = Pattern.compile("^(set|announce|start) (.+)");
        Matcher matcherOption = patternOption.matcher(arguments);
        if(matcherOption.find()) {
            String option = matcherOption.group(1);

            if(option.equalsIgnoreCase("set")) {
                Pattern patternSet = Pattern.compile("^set (\\w+) (.+)");
                Matcher matcherSet = patternSet.matcher(arguments);
                if(matcherSet.find()) {
                    String poll = "<https://goo.gl/forms/" + matcherSet.group(1) + ">";
                    String date = matcherSet.group(2);
                    String announcement = "The poll for the next movie night is now open! " + poll + " Movie night will be on " + date + ".";

                    Util.deleteMessage(commandEvent.getTextChannel().getMessageById(Long.parseLong(configManager.getConfigValue("mnID"))).complete());

                    MessageAction thingy = Util.sendBufferedMessage(commandEvent.getGuild().getTextChannelById(Channels.ANNOUNCEMENT_CH_ID.getId()), announcement);

                    Util.sendBufferedMessage(commandEvent.getGuild().getTextChannelById(Channels.GENERAL_CH_ID.getId()), announcement);
                    try {
                        configManager.setConfigValue("poll", poll);
                        configManager.setConfigValue("date", date);
                        configManager.setConfigValue("mnID", thingy);
                    } catch (Exception e) {
                        Util.reportHome(commandEvent.getMessage(), e);
                    }
                }

            } else if (option.equalsIgnoreCase("announce")) {
                Pattern patternAnnounce = Pattern.compile("^announce (.+)");
                Matcher matcherAnnounce = patternAnnounce.matcher(arguments);
                if (matcherAnnounce.find()) {
                    String movie = matcherAnnounce.group(1);
                    String announcement = "The movie night poll is now closed! The movie that won is " + movie + ". Movie night will be on " + configManager.getConfigValue("date");

                    Util.deleteMessage(commandEvent.getTextChannel().getMessageById(Long.parseLong(configManager.getConfigValue("mnID"))).complete());

                    MessageAction message2Delete = Util.sendBufferedMessage(commandEvent.getGuild().getTextChannelById(Channels.ANNOUNCEMENT_CH_ID.getId()), announcement);

                    Util.sendBufferedMessage(commandEvent.getGuild().getTextChannelById(Channels.GENERAL_CH_ID.getId()), announcement);
                    try {
                        configManager.setConfigValue("movie", movie);
                        configManager.setConfigValue("mnID", message2Delete);
                    } catch (Exception e) {
                        Util.reportHome(commandEvent.getMessage(), e);
                    }
                }

            } else if (option.equalsIgnoreCase("start")) {
                Pattern patternStart = Pattern.compile("^start (.+)");
                Matcher matcherStart = patternStart.matcher(arguments);
                if (matcherStart.find()) {
                    String rabbit = "<https://rabb.it/" + matcherStart.group(1) + ">";
                    String announcement = "We are about to start watching " + configManager.getConfigValue("movie") + "! Come check it out here " + rabbit;

                    Util.deleteMessage(commandEvent.getTextChannel().getMessageById(Long.parseLong(configManager.getConfigValue("mnID"))).complete());

                    MessageAction thingy = Util.sendBufferedMessage(commandEvent.getGuild().getTextChannelById(Channels.ANNOUNCEMENT_CH_ID.getId()), announcement);

                    Util.sendBufferedMessage(commandEvent.getGuild().getTextChannelById(Channels.GENERAL_CH_ID.getId()), announcement);
                    try {
                        configManager.setConfigValue("mnID", thingy);
                    } catch (Exception e) {
                        Util.reportHome(commandEvent.getMessage(), e);
                    }
                }
            }
            Util.deleteMessage(commandEvent.getMessage());
        }
    }
}