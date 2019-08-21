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
        String[] args = commandEvent.getArgs().split("\\s+", 1);
        String commandName = args[0];
        String commandResponse = commandEvent.getMessage().getContentRaw().split(" ", 3)[2];

        if(commandName != null && commandResponse != null && bot.getCommandManager().getCommandValue(commandName) == null && !bot.getRegisteredCommands().contains(commandName)) {
            bot.getCommandManager().setConfigValue(commandName, commandResponse);

            Util.simpleEmbed(commandEvent.getTextChannel(), "Custom command added: ``" + commandName + "``");
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
