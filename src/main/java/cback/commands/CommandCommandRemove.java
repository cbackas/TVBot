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
        String[] args = commandEvent.getArgs().split("\\s+", 1);
        if(args.length == 1) {
            String command = args[0];

            if(bot.getCommandManager().getCommandValue(command) != null) {
                bot.getCommandManager().removeConfigValue(command);
                Util.simpleEmbed(commandEvent.getTextChannel(), "Custom command removed: ``" + command + "``");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
