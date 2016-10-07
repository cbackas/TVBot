package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.*;

import java.util.EnumSet;
import java.util.List;

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
                List<IChannel> mentionsC = message.getChannelMentions();
                List<IUser> mentionsU = message.getMentions();
                List<IRole> mentionsG = message.getRoleMentions();
                String finalText = text;
                for (IChannel c : mentionsC) {
                    String displayName = "#" + c.getName();
                    finalText = text.replace(c.mention(), displayName).replace(c.mention(), displayName);
                }
                for (IUser u : mentionsU) {
                    String displayName = "@" + u.getDisplayName(guild);
                    finalText = finalText.replace(u.mention(false), displayName).replace(u.mention(true), displayName);
                }
                for (IRole g : mentionsG) {
                    String displayName = "@" + g.getName();
                    finalText = finalText.replace(g.mention(), displayName).replace(g.mention(), displayName);
                }
                Util.sendMessage(guild.getChannelByID("217456105679224846"), "```" + finalText + "\n- " + message.getAuthor().getDisplayName(guild) + "```");
                Util.sendMessage(message.getChannel(), "Log added.");
                Util.deleteMessage(message);
            } catch (Exception e) {
                Util.sendMessage(message.getChannel(), "You don't have permission to add logs.");
            }
        } else {
            Util.sendMessage(message.getChannel(), "Usage: !addlog <text>");
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
