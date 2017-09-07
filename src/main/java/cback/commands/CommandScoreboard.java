package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CommandScoreboard implements Command {
    @Override
    public String getName() {
        return "scoreboard";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("leaderboard", "topxp", "top");
    }

    @Override
    public String getSyntax() {
        return "scoreboard [#]";
    }

    @Override
    public String getDescription() {
        return "Shows a list of people with the most xp!";
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        int defaultCount = 5;
        if (args.length == 1 && Integer.parseInt(args[0]) > 5) {
            if (Integer.parseInt(args[0]) <= 30) {
                defaultCount = Integer.parseInt(args[0]);
            } else {
                defaultCount = 30;
            }
        }

        List<UserXP> topUsers = bot.getDatabaseManager().getXP().getTopUsers(defaultCount);
        if (topUsers != null && topUsers.size() > 0) {
            StringBuilder scoreboard = new StringBuilder();
            scoreboard.append("**Most Active Lounge Users**\n");

            Iterator<UserXP> userXPIterator = topUsers.iterator();
            int index = 0;
            while (userXPIterator.hasNext()) {
                UserXP userXP = userXPIterator.next();
                IUser user = userXP.getUser();
                if (user == null) continue;
                index++;

                scoreboard.append("**").append(index).append(".** ");
                scoreboard.append(user.getName());
                scoreboard.append(" (").append(userXP.getMessageCount()).append(")");

                if (userXPIterator.hasNext()) scoreboard.append("\n");
            }

            Util.simpleEmbed(message.getChannel(), scoreboard.toString());

        } else {
            Util.simpleEmbed(message.getChannel(), "No scoreboard data found.");
        }

        Util.deleteMessage(message);
    }

}
