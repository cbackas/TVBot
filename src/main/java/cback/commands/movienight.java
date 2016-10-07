package cback.commands;

import cback.TVBot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class movienight implements Command {
    @Override
    public String getName() {
        return "movienight";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {

    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
