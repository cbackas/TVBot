package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandLenny implements Command {
    @Override
    public String getName() {
        return "lenny";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        Util.sendMessage(message.getChannel(), "``" + message.getAuthor().getDisplayName(guild) + "``\n( ͡° ͜ʖ ͡°)");
        Util.deleteMessage(message);
    }

}
