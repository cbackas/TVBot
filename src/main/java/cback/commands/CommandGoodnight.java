package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class CommandGoodnight implements Command {
    @Override
    public String getName() {
        return "goodnight";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        Util.deleteMessage(message);
        Util.sendMessage(message.getChannel(), "``" + message.getAuthor().getDisplayName(guild) + "``\n\uD83D\uDE1A \uD83D\uDCA4 \uD83D\uDC4B \uD83C\uDF1B");
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
