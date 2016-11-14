package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

import java.util.Arrays;
import java.util.List;

public class CommandPurgePrivate implements Command {
    @Override
    public String getName() {
        return "purgeprivate";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("ppurge");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {

            IChannel privateChannel = guild.getChannelByID("240614159958540288");
            List<IMessage> messages = privateChannel.getMessages();
            Util.bulkDelete(privateChannel, messages);

            Util.botLog(message);
            Util.deleteMessage(message);

        }
    }

}
