package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCommandAdd implements Command {
    @Override
    public String getName() {
        return "addcommand";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    public static List<String> permitted = Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id, TVRoles.HEADMOD.id);

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<String> userRoles = message.getAuthor().getRolesForGuild(guild).stream().map(role ->role.getID()).collect(Collectors.toList());
        if (!Collections.disjoint(userRoles, permitted)) {

            String text = message.getContent();

            String commandName = args[0];
            String commandResponse = text.split(" ", 3)[2];

            if (commandName != null && commandResponse != null && bot.getCommandManager().getCommandValue(commandName) == null && !TVBot.getInstance().registeredCommands.contains(commandName)) {
                bot.getCommandManager().setConfigValue(commandName, commandResponse);

                Util.sendMessage(message.getChannel(), "Custom command added: ``" + commandName + "``");
            } else {
                Util.sendMessage(message.getChannel(), "**Usage**: ``!addcommand commandname \"custom response\"``");
            }

            Util.deleteMessage(message);
        }
    }
}
