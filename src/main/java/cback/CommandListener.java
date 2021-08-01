package cback;

import cback.commandsV2.Command;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Optional;

public class CommandListener extends ListenerAdapter {
    public ArrayList<Command> registeredCommands = new ArrayList<>();

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

