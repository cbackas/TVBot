package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;

public class CommandSuggestList implements Command{
    @Override
    public String getName() {
        return "seesuggestions";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("seeideas", "seesuggest");
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
        //Lounge Command Only
        if (guild.getID().equals("192441520178200577")) {
            try {
                List<IMessage> messages = Util.getSuggestions();

                StringBuilder response = new StringBuilder();
                if (!messages.isEmpty()) {
                    for (IMessage m : messages) {
                        String suggestion = m.getContent().split(" ", 2)[1];

                        if (suggestion != null) {
                            response.append("\n").append(suggestion);
                        }
                    }
                } else {
                    response.append("\n").append("There aren't currently any suggestions.");
                }

                Util.sendPrivateMessage(message.getAuthor(), "**Current Suggestions**:\n" + response.toString());
                Util.deleteMessage(message);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
