package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

public class CommandTrigger implements Command {
    @Override
    public String getName() {
        return "trigger";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getID().equals("73416411443113984")) {
            List<IUser> users = Util.getUsersByRole("226078206497783809");

            StringBuilder fMessage = new StringBuilder();
            for (IUser u : users) {
                fMessage.append("\n").append(u.getDisplayName(guild));
            }

            Util.sendMessage(message.getChannel(), "Users in the role that I'm testing:\n" + fMessage.toString());

            Util.deleteMessage(message);
        }
    }

}
