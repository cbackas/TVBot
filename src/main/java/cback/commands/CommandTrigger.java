package cback.commands;

import cback.TVBot;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;


public class CommandTrigger extends Command {

    private TVBot bot;

    public CommandTrigger() {
        this.bot = TVBot.getInstance();
        this.name = "trigger";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        if(commandEvent.getAuthor().getId().equals("73416411443113984")) {
            String text = String.valueOf(Util.getCurrentTime());
            Util.sendMessage(commandEvent.getTextChannel(), text);

            Util.deleteMessage(commandEvent.getMessage());
        }
    }
}