package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;
import java.util.Optional;

public class CommandRoleGet extends Command {

    private TVBot bot;

    public CommandRoleGet() {
        this.bot = TVBot.getInstance();
        this.name = "roleid";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        if(args.length >= 1) {
            String roleName = String.join(" ", args);
            List<Role> serverRoles = commandEvent.getGuild().getRoles();

            if(args[0].equalsIgnoreCase("listall")) {
                String roleList = serverRoles.stream().map(role -> role.getName() + " " + role.getId()).reduce("", (a, b) -> a + b + "\n");
                Util.sendMessage(commandEvent.getTextChannel(), roleList);
            } else {
                Optional<Role> foundRole = serverRoles.stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).findAny();
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