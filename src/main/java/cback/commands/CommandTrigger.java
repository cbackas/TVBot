package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IDiscordObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTrigger implements Command {
    @Override
    public String getName() {
        return "trigger";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getID().equals("73416411443113984")) {
            List<IChannel> permChannels = bot.getConfigManager().getConfigArray("permanentchannels").stream().map(id -> guild.getChannelByID(id)).collect(Collectors.toList());
            List<IChannel> allChannels = guild.getChannels();
            permChannels.forEach(allChannels::remove);

            List<String> channelNames = new ArrayList<>();
            channelNames.addAll(allChannels.stream().map(IChannel::getName).collect(Collectors.toList()));
            channelNames.sort((p1, p2) -> p1.compareTo(p2));

            for (String c : channelNames) {
                int position = channelNames.indexOf(c) + 15;
                RequestBuffer.request(() -> {
                    try {
                        guild.getChannelsByName(c).get(0).changePosition(position);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }


            Util.deleteMessage(message);
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
