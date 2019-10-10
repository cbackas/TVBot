package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandCommandRemove extends Command {

    private TVBot bot;

    public CommandCommandRemove() {
        this.bot = TVBot.getInstance();
        this.name = "removecommand";
        this.aliases = new String[]{"rcom"};
        this.arguments = "removecommand [commandname]";
        this.help = "Deletes a custom command form the official custom command database!";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());
        if(args.length == 1) {
            String command = args[0];

            if (bot.getCustomCommandManager().getCommandValue(command) != null) {
                bot.getCustomCommandManager().removeConfigValue(command);
                Util.simpleEmbed(commandEvent.getTextChannel(), "Custom command removed: ``" + command + "``");
            } else {
                Util.simpleEmbed(commandEvent.getTextChannel(), "That command doesn't exist.");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
