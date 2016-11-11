package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;

public class CommandSeeSuggestions implements Command{
    @Override
    public String getName() {
        return "seesuggestions";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("seeideas", "seesuggest");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        try {
            IChannel channel = client.getGuildByID("192441520178200577").getChannelByID("192444470942236672");

            List<IMessage> messages = channel.getPinnedMessages();
            List<IMessage> permM = Arrays.asList(channel.getMessageByID("228166713521340416"), channel.getMessageByID("236703936789217285"), channel.getMessageByID("246306837748514826"));
            permM.forEach(messages::remove);

            StringBuilder response = new StringBuilder();
            for (IMessage m : messages) {
                String text = m.getContent();
                String suggestion[] = text.split(" ",2);

                response.append("\n").append(suggestion[1]);
            }

            Util.sendPrivateMessage(message.getAuthor(), "**Current Suggestions**:\n" + response.toString());
            Util.deleteMessage(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
