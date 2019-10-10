package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.xp.UserXP;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.User;


public class CommandXPAdd extends Command {

    private TVBot bot;

    public CommandXPAdd() {
        this.bot = TVBot.getInstance();
        this.name = "addxp";
        this.arguments = "addxp @user #";
        this.help = "Adds a certain number of xp to the desired user.";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        if(args.length >= 1) {
            User mentioned = Util.getUserFromMentionArg(args[0]);
            if(mentioned != null) {
                UserXP xp = bot.getDatabaseManager().getXP().getUserXP(mentioned.getId());
                if(xp != null) {
                    int number = Integer.parseInt(args[1]);

                    bot.getDatabaseManager().getXP().addXP(mentioned.getId(), number);
                    Util.simpleEmbed(commandEvent.getTextChannel(), "Granted " + number + " xp to " + mentioned.getName());
                } else {
                    Util.simpleEmbed(commandEvent.getTextChannel(), "No xp data found for " + mentioned.getName());
                }
            } else {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Invalid user \"" + args[0] + "\".");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
