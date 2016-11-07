package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {
            if (args.length == 1) {
                String roleName = Arrays.stream(args).collect(Collectors.joining(" "));
                List<IRole> serverRoles = guild.getRoles();

                if (roleName.equalsIgnoreCase("listall")) {
                    String roleList = serverRoles.stream().map(role -> role.getName() + " " + role.getID()).reduce("",(a, b) -> a + b + "\n");

                    Util.sendBufferedMessage(message.getChannel(), roleList);
                } else {
                    Optional<IRole> foundRole = serverRoles.stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).findAny();

                    if (foundRole.isPresent()) {
                        Util.sendMessage(message.getChannel(), "Found id for **" + foundRole.get().getName() + "**: " + foundRole.get().getID());

                    } else {
                        Util.sendMessage(message.getChannel(), "Role not found");
                    }
                }

                Util.deleteMessage(message);
                Util.botLog(message);

            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
