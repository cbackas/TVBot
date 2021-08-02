package cback;

import cback.commandsV2.Command;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import org.reflections.Reflections;

import java.util.*;

public class CommandListener extends ListenerAdapter {
    protected ArrayList<Command> registeredCommands = new ArrayList<>();

    public CommandListener() {
        registerAllCommands();
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        Optional<Command> registeredCommand = registeredCommands.stream()
                .filter(com -> com.getCommandData().getName().equals(event.getName()))
                .findAny();
        if (registeredCommand.isPresent()) {
            Command command = registeredCommand.get();
            command.execute(event);
            Util.getLogger().info("Executed " + command.getCommandData().getName());
        }
    }

    private void registerAllCommands() {
        new Reflections("cback.commandsV2").getSubTypesOf(Command.class).forEach(commandImpl -> {
            try {
                Command command = commandImpl.getDeclaredConstructor().newInstance();
                String commandName = command.getCommandData().getName();
                Optional<Command> existingCommand = registeredCommands.stream()
                        .filter(cmd -> commandName.equalsIgnoreCase(cmd.getCommandData().getName()))
                        .findAny();
                if (existingCommand.isEmpty()) {
                    // give admin perms to all non default-open commands
                    CommandPrivilege adminPriv = new CommandPrivilege(CommandPrivilege.Type.ROLE, true, TVRoles.ADMIN.id);
                    if (
                            !command.commandPrivileges.contains(adminPriv) &&
                                    command.commandPrivileges.size() > 0 &&
                                    command.commandPrivileges.size() <= 10
                    ) {
                        command.commandPrivileges.add(adminPriv);
                    }

                    registeredCommands.add(command);
                    Util.getLogger().info("Registered Slash Command: " + commandName);
                } else {
                    Util.getLogger().error("Attempted to register two commands with the same name: " + commandName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

