package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;

public class CommandDeleteChannel implements Command {
    @Override
    public String getName() {
        return "deletechannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("removechannel");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {
            List<IChannel> mentionsC = message.getChannelMentions();
            for (IChannel c : mentionsC) {
                try {
                    c.delete();
                    Util.sendBufferedMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```Deleted " + c.getName() + " channel.\n- " + message.getAuthor().getDisplayName(guild) + "```");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Util.deleteMessage(message);
            Util.botLog(message);
        }
    }

}
