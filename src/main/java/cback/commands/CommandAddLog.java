package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class CommandAddLog implements Command {
    @Override
    public String getName() {
        return "addlog";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length >= 1) {
            String text = message.getContent().split(" ", 2)[1];
            Util.sendMessage(client.getChannelByID("217456105679224846"), "```" + text + "```");
        } else {
            Util.sendMessage(message.getChannel(), "Usage: !status <text>");
        }
    }
}
