package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class CommandShrug implements Command {
    @Override
    public String getName() {
        return "shrug";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        Util.deleteMessage(message);
        Util.sendMessage(message.getChannel(), "``" + message.getAuthor().getDisplayName(guild) + "``\n¯\\_(ツ)_/¯");
    }
}
