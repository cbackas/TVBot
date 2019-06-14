package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandCommandAdd extends Command {

    private TVBot bot;

    public CommandCommandAdd(TVBot bot) {
        this.bot = bot;
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

        if(commandName != null && commandResponse != null && bot.getCommandManager().getCommandValue(commandName) == null && !TVBot.registeredCommands.contains(commandName)) {
            bot.getCommandManager().setConfigValue(commandName, commandResponse);

            Util.simpleEmbed(commandEvent.getChannel(), "Custom command added: ``" + commandName + "``");
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
