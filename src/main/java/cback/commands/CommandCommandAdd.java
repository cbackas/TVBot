package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandCommandAdd implements Command {
    @Override
    public String getName() {
        return "addcommand";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {

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
