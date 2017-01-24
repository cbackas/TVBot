package cback.commands;

import cback.TVBot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;

public class CommandSuggest implements Command {
    @Override
    public String getName() {
        return "suggest";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("idea","suggestion");
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
        if (message.getChannel().getID().equals("192444470942236672") || message.getChannel().getID().equals("214763749867913216") || message.getChannel().getID().equals("256491839870337024")) {
            try {
                message.getChannel().pin(client.getMessageByID(message.getID()));
            } catch (Exception e) {
            }
        }
    }

}
