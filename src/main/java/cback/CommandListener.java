package cback;

import cback.commandsV2.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        var loungeID = TVBot.getInstance().getConfigManager().getConfigValue("HOMESERVER_ID");
        Guild homeGuild = event.getJDA().getGuildById(Long.parseLong(loungeID));

        // register the slash commands with discord
        homeGuild.updateCommands()
                .addCommands(
                        // pull the commandDatas out of the commands
                        this.registeredCommands.stream()
                                .map(cback.commandsV2.Command::getCommandData)
                                .collect(Collectors.toList())
                )
                .submit()
                .thenCompose(cmdList -> {
                    // get commands back from discord
                    // then build map connecting IDs to registeredCommand's privileges
                    // send privileges off to discord
                    Map<String, Collection<? extends CommandPrivilege>> privileges = new HashMap<>();
                    ArrayList<Command> cmds = new ArrayList<>(this.registeredCommands);

                    cmdList.forEach(cmd -> {
                        for (cback.commandsV2.Command registeredCommand : cmds) {
                            if (cmd.getName().equals(registeredCommand.getCommandData().getName())) {
                                if (registeredCommand.getCommandPrivileges().isEmpty()) continue;

                                privileges.putIfAbsent(cmd.getId(), registeredCommand.getCommandPrivileges());
                                cmds.remove(registeredCommand);

                                break;
                            }
                        }
                    });

                    return homeGuild.updateCommandPrivileges(privileges).submit();
                })
                .whenComplete((v, error) -> {
                    if (error != null) Util.getLogger().error("Failed to submit privileges", error);
                });
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

