package cback;

import cback.commandsV2.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandListener extends ListenerAdapter {
    protected ArrayList<Command> registeredCommands = new ArrayList<>();

    public CommandListener() {
        registerAllCommands();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        Guild homeGuild = TVBot.getInstance().getHomeGuild();

        // register the slash commands with discord
        homeGuild.updateCommands()
                .addCommands(
                        registeredCommands.stream()
                                .map(Command::getCommandData)
                                .collect(Collectors.toList())
                )
                .queue();

        Map<String, Collection<? extends CommandPrivilege>> privileges = new HashMap<>();
        registeredCommands.forEach(cmd -> {
            if (cmd.getCommandPrivileges() != null) {
                privileges.putIfAbsent(cmd.getCommandData().getName(), cmd.getCommandPrivileges());
            }
        });
        System.out.println(privileges);
//        homeGuild.updateCommandPrivileges(privileges);

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

