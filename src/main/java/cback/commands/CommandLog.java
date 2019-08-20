package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;


public class CommandLog extends Command {

    private TVBot bot;

    public CommandLog() {
        this.bot = TVBot.getInstance();
        this.name = "addlog";
        this.aliases = new String[]{"log"};
        this.arguments = "addlog [message]";
        this.help = "Submits a serverlog with some info attached";
        this.requiredRole = TVRoles.STAFF.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        if(args.length >= 1) {
            String finalText = commandEvent.getMessage().getContentDisplay().split(" ", 2)[1];
            Util.sendLog(commandEvent.getMessage(), finalText);
            Util.simpleEmbed(commandEvent.getTextChannel(), "Log added. " + commandEvent.getGuild().getTextChannelById(Channels.SERVERLOG_CH_ID.getId()).getAsMention());
            Util.deleteMessage(commandEvent.getMessage());
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }
}