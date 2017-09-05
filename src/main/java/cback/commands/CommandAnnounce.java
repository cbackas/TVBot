package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;

public class CommandAnnounce implements Command {
    @Override
    public String getName() {
        return "announce";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "announce [message]";
    }

    @Override
    public String getDescription() {
        return "Sends a message in the announcement channel and the general channel";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length >= 1) {
            String announcement = message.getContent().split(" ", 2)[1];
            Util.sendAnnouncement(announcement);
        } else {
            Util.syntaxError(this, message);
        }
        Util.deleteMessage(message);
    }

}
