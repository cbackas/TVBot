package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Member;

import java.util.Iterator;
import java.util.List;

public class CommandScoreboard extends Command {

    private TVBot bot;

    public CommandScoreboard() {
        this.bot = TVBot.getInstance();
        this.name = "scoreboard";
        this.aliases = new String[]{"leaderboard", "topxp", "top"};
        this.arguments = "scoreboard [#]";
        this.help = "Shows a list of people with the most xp!";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        int defaultCount = 5;
        if(args.length == 1 && Integer.parseInt(args[0]) > 5) {
            defaultCount = Math.min(Integer.parseInt(args[0]), 30);
        }

        List<UserXP> topUsers = bot.getDatabaseManager().getXP().getTopUsers(defaultCount);
        if(topUsers != null && topUsers.size() > 0) {
            StringBuilder scoreboard = new StringBuilder();
            scoreboard.append("**Most Active Lounge Users**\n");

            Iterator<UserXP> userXPIterator = topUsers.iterator();
            int index = 0;
            while(userXPIterator.hasNext()) {
                UserXP userXP = userXPIterator.next();
                Member user = userXP.getUser();
                if(user == null) continue;
                index++;

                scoreboard.append("**").append(index).append(".** ");
                scoreboard.append(user.getEffectiveName());
                scoreboard.append(" (").append(userXP.getMessageCount()).append(")");

                if(userXPIterator.hasNext()) scoreboard.append("\n");
            }
            Util.simpleEmbed(commandEvent.getTextChannel(), scoreboard.toString());
        } else {
            Util.simpleEmbed(commandEvent.getTextChannel(), "No scoreboard data found.");
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}