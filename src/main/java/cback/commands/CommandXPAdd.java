package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.xp.UserXP;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.entities.Member;


public class CommandXPAdd extends Command {

    private TVBot bot;

    public CommandXPAdd(TVBot bot) {
        this.bot = bot;
        this.name = "addxp";
        this.arguments = "addxp @user #";
        this.help = "Adds a certain number of xp to the desired user.";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        if(args.length >= 1) {
            Member mentioned = Util.getUserFromMentionArg(args[0]);
            if(mentioned != null) {
                UserXP xp = bot.getDatabaseManager().getXP().getUserXP(mentioned.getUser().getId());
                if(xp != null) {
                    int number = Integer.parseInt(args[1]);

                    bot.getDatabaseManager().getXP().addXP(mentioned.getUser().getId(), number);
                    Util.simpleEmbed(commandEvent.getChannel(), "Granted " + number + " xp to " + mentioned.getEffectiveName());
                } else {
                    Util.simpleEmbed(commandEvent.getChannel(), "No xp data found for " + mentioned.getEffectiveName());
                }
            } else {
                Util.simpleEmbed(commandEvent.getChannel(), "Invalid user \"" + args[0] + "\".");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
