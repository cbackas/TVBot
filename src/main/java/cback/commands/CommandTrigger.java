package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.util.List;

public class CommandTrigger extends Command {

    private TVBot bot;

    public CommandTrigger(TVBot bot) {
        this.bot = bot;
        this.name = "trigger";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        if(commandEvent.getAuthor().getId().equals("73416411443113984")) {
            String text = String.valueOf(Util.getCurrentTime());
            Util.sendMessage(commandEvent.getChannel(), text);

            Util.deleteMessage(commandEvent.getMessage());
        }
    }
}