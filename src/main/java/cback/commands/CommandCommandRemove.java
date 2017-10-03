package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;

public class CommandCommandRemove implements Command {
    @Override
    public String getName() {
        return "removecommand";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("rcom");
    }

    @Override
    public String getSyntax() {
        return "removecommand [commandname]";
    }

    @Override
    public String getDescription() {
        return "Deletes a custom command from the official custom command database!";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length == 1) {
            String command = args[0];

            if (bot.getCommandManager().getCommandValue(command) != null) {
                bot.getCommandManager().removeConfigValue(command);

                Util.simpleEmbed(message.getChannel(), "Custom command removed: ``" + command + "``");
            }
        } else {
            Util.syntaxError(this, message);
        }

        Util.deleteMessage(message);
    }
}
