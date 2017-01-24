package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandCommandRemove implements Command {
    @Override
    public String getName() {
        return "removecommand";
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
    public List<String> getPermissions() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVRoles.ADMIN.id))) {

            if (args.length == 1) {

                String command = args[0];

                if (bot.getCommandManager().getCommandValue(command) != null) {
                    bot.getCommandManager().removeConfigValue(command);

                    Util.sendMessage(message.getChannel(), "Custom command removed: ``" + command + "``");
                }


            } else {
                Util.sendMessage(message.getChannel(), "**Usage**: ``!removecommand commandname``");
            }

            Util.deleteMessage(message);
        }
    }
}
