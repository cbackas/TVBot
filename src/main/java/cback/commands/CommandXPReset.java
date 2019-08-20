package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.xp.UserXP;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public class CommandXPReset extends Command {

    private TVBot bot;

    public CommandXPReset() {
        this.bot = TVBot.getInstance();
        this.name = "resetxp";
        this.arguments = "resetxp @user";
        this.help = "Sets a user's xp to 0. Sucks to be them.";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        if(args.length >= 1) {
            User mentioned = Util.getUserFromMentionArg(args[0]);
            if(mentioned != null) {
                UserXP xp = bot.getDatabaseManager().getXP().getUserXP(mentioned.getId());
                if(xp != null) {
                    xp.setMessageCount(0);
                    bot.getDatabaseManager().getXP().updateUserXP(xp);
                    Util.simpleEmbed(commandEvent.getTextChannel(), "Reset xp for " + mentioned.getName());
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