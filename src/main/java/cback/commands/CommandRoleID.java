package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

import java.util.List;

public class CommandRoleID implements Command {
    @Override
    public String getName() {
        return "roleid";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getID().equals("73416411443113984")) {
            if (args.length == 1) {
                String roleName = args[0];
                List<IRole> serverRoles = guild.getRoles();
                for (IRole roles : serverRoles) {
                    if (roleName.equalsIgnoreCase("listall")) {
                        Util.sendBufferedMessage(message.getChannel(), roles.getName() + " " + roles.getID());
                    } else {
                        if (roles.getName().equalsIgnoreCase(roleName)) {
                            Util.sendMessage(message.getChannel(), "Found id for **" + roleName + "**: " + roles.getID());

                        } else {
                            Util.sendMessage(message.getChannel(), "Role not found");
                        }
                    }
                }
                Util.deleteMessage(message);
            }
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
