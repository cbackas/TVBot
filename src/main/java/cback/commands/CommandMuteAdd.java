package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMuteAdd extends Command {

    private TVBot bot;

    public CommandMuteAdd() {
        this.bot = TVBot.getInstance();
        this.name = "mute";
        this.arguments = "mute @user [reason?]";
        this.help = "Mutes a user and logs the action";
        this.requiredRole = TVRoles.STAFF.name;
    }
    
    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");

        Role muteRole = commandEvent.getGuild().getRolesByName("muted", true).get(0);

        if(args[0].equalsIgnoreCase("list")) {
            StringBuilder mutedList = new StringBuilder();
            if(!mutedUsers.isEmpty()) {
                for(String userID : mutedUsers) {
                    Member userO = commandEvent.getGuild().getMemberById(Long.parseLong(userID));
                    String user = "<@" + commandEvent.getGuild().getMemberById(Long.parseLong(userID));

                    if(userO != null) {
                        user = userO.getAsMention();
                    }
                    mutedList.append("\n").append(user);
                }
            } else {
                mutedList.append("\n").append("There are currently no mutes users.");
            }
            Util.simpleEmbed(commandEvent.getTextChannel(), "Muted Users: (plain text for users not on server)\n" + mutedList.toString());
        } else if(args.length >= 1) {
            Pattern pattern = Pattern.compile("^!mute <@!?(\\d+)> ?(.+)?");
            Matcher matcher = pattern.matcher(commandEvent.getMessage().getContentRaw());

            if(matcher.find()) {
                String u = matcher.group(1);
                String reason = matcher.group(2);

                Member userInput = commandEvent.getGuild().getMemberById(Long.parseLong(u));
                if (userInput != null) {
                    if (reason == null) {
                        reason = "an unspecified reason";
                    }

                    if (commandEvent.getAuthor().getId().equals(u)) {
                        Util.simpleEmbed(commandEvent.getTextChannel(), "You probably shouldn't mute yourself");
                    } else {
                        try {
                            commandEvent.getGuild().getController().addSingleRoleToMember(userInput, muteRole).queue();
                            Util.simpleEmbed(commandEvent.getTextChannel(), userInput.getEffectiveName() + " has been muted. Check " + commandEvent.getGuild().getTextChannelById(Channels.SERVERLOG_CH_ID.getId()).getAsMention() + " for more info.");
                            if (!mutedUsers.contains(u)) {
                                mutedUsers.add(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);
                            }

                            Util.sendLog(commandEvent.getMessage(), "Muted " + userInput.getEffectiveName() + "\n**Reason:** " + reason, Color.gray);
                        } catch (Exception e) {
                            Util.simpleEmbed(commandEvent.getTextChannel(), "Error running " + this.getName() + " - error recorded");
                            Util.reportHome(commandEvent.getMessage(), e);
                        }
                    }
                }
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}