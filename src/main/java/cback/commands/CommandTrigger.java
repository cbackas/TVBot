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
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        Util.getLogger().info("trigger");
        if (commandEvent.getAuthor().getId().equals("73416411443113984") || commandEvent.getAuthor().getId().equalsIgnoreCase("109109946565537792")) {

            var cat = commandEvent.getJDA().getCategoryById(TVBot.UNSORTED_CAT_ID);
            Util.sendMessage(commandEvent.getTextChannel(), "Null: " + (cat == null));
        }
    }
}