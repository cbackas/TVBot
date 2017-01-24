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
        return Arrays.asList("leaderboard", "topxp");
    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public List<String> getPermissions() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {

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
                index++;
                UserXP userXP = userXPIterator.next();
                IUser user = userXP.getUser();

                String name = "NULL";
                if (user != null) {
                    String displayName = user.getDisplayName(guild);
                    name = displayName != null ? displayName : user.getName();
                } else {
                    name = Util.requestUsernameByID(userXP.getUserID());
                }

                scoreboard.append("**").append(index).append(".** ");
                scoreboard.append(name);
                scoreboard.append(" (").append(userXP.getMessageCount()).append(")");

                if (userXPIterator.hasNext()) scoreboard.append("\n");
            }

            Util.sendMessage(message.getChannel(), scoreboard.toString());

        } else {
            Util.sendMessage(message.getChannel(), "No scoreboard data found.");
        }

        Util.deleteMessage(message);
    }

}
