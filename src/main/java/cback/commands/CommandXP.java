package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.entities.Member;

public class CommandXP extends Command {

    private TVBot bot;

    public CommandXP(TVBot bot) {
        this.bot = bot;
        this.name = "xp";
        this.arguments = "xp [@user]";
        this.help = "Shows you your xp or the person you mentioned's xp";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        Member user;
        if(args.length >= 1) {
            user = Util.getUserFromMentionArg(args[0]);
        } else {
            user = commandEvent.getMember();
        }

        if(user != null) {
            UserXP xp = bot.getDatabaseManager().getXP().getUserXP(user.getGuild().getId());
            if(xp != null) {
                Util.simpleEmbed(commandEvent.getChannel(), "**" + user.getGuild().getName() + "** ( Rank **#" + xp.getRank() + "** )\nXP: `" + xp .getMessageCount() + "`");
            } else {
                Util.simpleEmbed(commandEvent.getChannel(), " No xp data found for " + user.getGuild().getName());
            }
        } else {
            Util.simpleEmbed(commandEvent.getChannel(), "Invalid user \"" + args[0] + "\". You muse use an @mention");
        } Util.deleteMessage(commandEvent.getMessage());
    }
}