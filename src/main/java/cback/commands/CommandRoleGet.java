package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Role;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandRoleGet extends Command {

    private TVBot bot;

    public CommandRoleGet() {
        this.bot = TVBot.getInstance();
        this.name = "roleid";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        if(args.length == 1) {
            String roleName = String.join(" ", args);
            List<Role> serverRole = commandEvent.getGuild().getRoles();

            if(roleName.equalsIgnoreCase("listall")) {
                String roleList = serverRole.stream().map(role -> role.getName() + " " + role.getId()).reduce("", (a, b) -> a + b + "\n");
                Util.sendBufferedMessage(commandEvent.getTextChannel(), roleList);
            } else {
                Optional<Role> foundRole = serverRole.stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).findAny();
                if(foundRole.isPresent()) {
                    Util.simpleEmbed(commandEvent.getTextChannel(), "Found id for **" + foundRole.get().getName() + "**: " + foundRole.get().getId());
                } else {
                    Util.simpleEmbed(commandEvent.getTextChannel(), "Role not found");
                }
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}