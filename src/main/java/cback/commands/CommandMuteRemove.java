package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Member;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMuteRemove extends Command {

    private TVBot bot;

    public CommandMuteRemove(TVBot bot) {
        this.bot = bot;
        this.name = "unmute";
        this.arguments = "unmute @user";
        this.help = "Unmutes a user";
        this.requiredRole = TVRoles.STAFF.name;
    }
    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        if(args.length == 1) {
            String user = args[0];
            Pattern pattern = Pattern.compile("^<@!?(\\d+)>");
            Matcher matcher = pattern.matcher(user);
            if(matcher.find()) {
                String u = matcher.group(1);
                Member userInput = commandEvent.getGuild().getMemberById(Long.parseLong(u));
                if(userInput != null) {
                    if(commandEvent.getAuthor().getId().equals(u)) {
                        Util.sendMessage(commandEvent.getChannel(), "Not sure how you typed this command... but you can't unmute yourself");
                    } else {
                        try {
                            userInput.getRoles().remove(commandEvent.getGuild().getRoleById(231269949635559424L));

                            Util.simpleEmbed(commandEvent.getChannel(), userInput.getEffectiveName() + "has been unmuted");

                            List<String> mutedUsers = bot.getConfigManager().getConfigArray("muted");
                            if(mutedUsers.contains(u)) {
                                mutedUsers.remove(u);
                                bot.getConfigManager().setConfigValue("muted", mutedUsers);
                            }
                            Util.sendLog(commandEvent.getMessage(), userInput.getEffectiveName() + "has been unmuted.", Color.gray);
                        } catch(Exception ex) {
                            Util.simpleEmbed(commandEvent.getChannel(), "Error running " + this.getName() + " - error recorded");
                            Util.reportHome(commandEvent.getMessage(), ex);
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