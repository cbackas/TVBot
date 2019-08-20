package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandCommandList extends Command {

    private TVBot bot;

    public CommandCommandList() {
        this.bot = TVBot.getInstance();
        this.name = "listcommands";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        Util.simpleEmbed(commandEvent.getTextChannel(), "**Custom Commands**: \n" + bot.getCommandManager().getCommandList());
        Util.deleteMessage(commandEvent.getMessage());
    }
}
