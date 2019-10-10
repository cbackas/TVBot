package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.User;

public class CommandXP extends Command {

    private TVBot bot;

    public CommandXP() {
        this.bot = TVBot.getInstance();
        this.name = "xp";
        this.arguments = "xp [@user]";
        this.help = "Shows you your xp or the person you mentioned's xp";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        User user;
        if(args.length >= 1) {
            user = Util.getUserFromMentionArg(args[0]);
        } else {
            user = commandEvent.getAuthor();
        }

        if(user != null) {
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getId());
            if(xp != null) {
                Util.simpleEmbed(commandEvent.getTextChannel(), "**" + user.getName() + "** ( Rank **#" + xp.getRank() + "** )\nXP: `" + xp .getMessageCount() + "`");
            } else {
                Util.simpleEmbed(commandEvent.getTextChannel(), " No xp data found for " + user.getName());
            }
        } else {
            Util.simpleEmbed(commandEvent.getTextChannel(), "Invalid user \"" + args[0] + "\". You muse use an @mention");
        } Util.deleteMessage(commandEvent.getMessage());
    }
}