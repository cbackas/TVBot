package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

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
        return "suggest [suggestion text]";
    }

    @Override
    public String getDescription() {
        return "Pins your message, making it an official suggestion.";
    }
    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<Long> suggestable = Arrays.asList(256491839870337024l, 192444470942236672l, 256491839870337024l);
        if (suggestable.contains(message.getChannel().getLongID())) {
            try {
                message.getChannel().pin(message);
            } catch (Exception e) {
                Util.reportHome(message, e);
            }
        }
    }

}
