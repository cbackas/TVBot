package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
import java.util.List;

public class CommandAddPrivate implements Command {
    @Override
    public String getName() {
        return "addprivate";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("padd");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {

            List<IUser> users = message.getMentions();
            for (IUser u : users) {
                RequestBuffer.request(() -> {
                try {
                    u.addRole(guild.getRoleByID("241767985302208513"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                });
            }

            Util.sendMessage(guild.getChannelByID("240614159958540288"), "Added user(s) to private channel.");

            Util.botLog(message);
            Util.deleteMessage(message);
        }
    }

}
