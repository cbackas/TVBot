package cback.commands;

import cback.TVbot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public interface Command {
    String getName();

    void execute(TVbot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate);
}

