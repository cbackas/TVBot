package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandRoleGet implements Command {
    @Override
    public String getName() {
        return "roleid";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length == 1) {
            String roleName = Arrays.stream(args).collect(Collectors.joining(" "));
            List<IRole> serverRoles = guild.getRoles();

            if (roleName.equalsIgnoreCase("listall")) {
                String roleList = serverRoles.stream().map(role -> role.getName() + " " + role.getStringID()).reduce("", (a, b) -> a + b + "\n");

                Util.sendBufferedMessage(message.getChannel(), roleList);
            } else {
                Optional<IRole> foundRole = serverRoles.stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).findAny();

                if (foundRole.isPresent()) {
                    Util.simpleEmbed(message.getChannel(), "Found id for **" + foundRole.get().getName() + "**: " + foundRole.get().getStringID());
                } else {
                    Util.simpleEmbed(message.getChannel(), "Role not found");
                }
            }
        } else {
            Util.syntaxError(this, message);
        }
        Util.deleteMessage(message);
    }
}