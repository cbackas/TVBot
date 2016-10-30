package cback.commands;

import cback.TVBot;
import cback.Util;
import cback.database.xp.UserXP;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Iterator;
import java.util.List;

public class CommandScoreboard implements Command {
    @Override
    public String getName() {
        return "scoreboard";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {

        List<UserXP> topUsers = bot.getDatabaseManager().getXP().getTopUsers(5);
        if (topUsers != null && topUsers.size() > 0) {
            StringBuilder scoreboard = new StringBuilder();
            scoreboard.append("**Most Active Lounge Users**\n");

            Iterator<UserXP> userXPIterator = topUsers.iterator();
            int index = 0;
            while(userXPIterator.hasNext()){
                index++;
                UserXP user = userXPIterator.next();
                scoreboard.append("**").append(index).append(".** ");
                scoreboard.append(user.getUser().getDisplayName(guild));
                scoreboard.append(" (").append(user.getMessageCount()).append(")");
                if(userXPIterator.hasNext()) scoreboard.append("\n");
            }

            Util.sendMessage(message.getChannel(), scoreboard.toString());

        } else {
            Util.sendMessage(message.getChannel(), "No scoreboard data found.");
        }

        Util.deleteMessage(message);
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
