package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.EnumSet;

public class CommandAddLog implements Command {
    @Override
    public String getName() {
        return "addlog";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length >= 1) {
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                String text = message.getContent().split(" ", 2)[1];
                Util.sendMessage(client.getChannelByID("217456105679224846"), "```" + text + "```");
            } catch (Exception e) {
                Util.sendMessage(message.getChannel(), "You don't have permission to add logs.");
            }
        } else {
            Util.sendMessage(message.getChannel(), "Usage: !addlog <text>");
        }
    }
}
