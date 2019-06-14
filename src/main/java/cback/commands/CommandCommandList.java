package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandCommandList extends Command {

    private TVBot bot;

    public CommandCommandList(TVBot bot) {
        this.bot = bot;
        this.name = "listcommands";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        Util.simpleEmbed(commandEvent.getChannel(), "**Custom Commands**: \n" + bot.getCommandManager().getCommandList());
        Util.deleteMessage(commandEvent.getMessage());
    }
}
