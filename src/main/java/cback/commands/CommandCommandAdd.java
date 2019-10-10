package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandCommandAdd extends Command {

    private TVBot bot;

    public CommandCommandAdd() {
        this.bot = TVBot.getInstance();
        this.name = "addcommand";
        this.arguments = "addcommand commandname \"custom response\"";
        this.help = "Creates a simple custom command";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs(), 2);
        String commandName = args[0];

        if (args.length == 2) {
            if (bot.getCustomCommandManager().getCommandValue(commandName) != null) {
                Util.simpleEmbed(commandEvent.getTextChannel(), "This command already exists.");
            } else {
                bot.getCustomCommandManager().setConfigValue(commandName, args[1]);
                Util.simpleEmbed(commandEvent.getTextChannel(), "Custom command added: ``" + commandName + "``");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
