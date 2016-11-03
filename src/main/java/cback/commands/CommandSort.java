package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CommandSort implements Command {

    @Override
    public String getName() {
        return "sort";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {

        if (bot.getBotAdmins().contains(message.getAuthor().getID())) {

            //permanent channels sorted by position to keep on top or bottom
            List<IChannel> permChannels = bot.getConfigManager().getConfigArray("permanentchannels").stream()
                    .map(id -> guild.getChannelByID(id))
                    .sorted((chan1, chan2) -> Integer.compare(chan1.getPosition(), chan2.getPosition()))
                    .collect(Collectors.toList());
            List<IChannel> permChannelsTop = new ArrayList<>();
            List<IChannel> permChannelsBottom = new ArrayList<>();
            //add top and bottom channels to their respective lists
            IntStream.range(0, permChannels.size())
                    .forEach(index -> {
                        IChannel channel = permChannels.get(index);
                        if (channel.getPosition() == index)
                            permChannelsTop.add(channel);
                        else
                            permChannelsBottom.add(channel);

                    });

            //sort non permanent channels
            List<IChannel> showsChannelsSorted = guild.getChannels().stream()
                    .filter(chan -> !permChannels.contains(chan))
                    .sorted((chan1, chan2) -> getSortName(chan1.getName()).compareTo(getSortName(chan2.getName())))
                    .collect(Collectors.toList());

            //add newly sorted channels in order
            List<IChannel> allChannelsSorted = new ArrayList<>();
            allChannelsSorted.addAll(permChannelsTop);
            allChannelsSorted.addAll(showsChannelsSorted);
            allChannelsSorted.addAll(permChannelsBottom);

            //apply new positions
            IntStream.range(0, allChannelsSorted.size()).forEach(position -> {
                IChannel channel = allChannelsSorted.get(position);
                if (!(channel.getPosition() == position)) //don't sort if position is already correct
                    RequestBuffer.request(() -> {
                        try {
                            channel.changePosition(position);
                        } catch (DiscordException e) {
                            e.printStackTrace();
                        } catch (MissingPermissionsException e) {
                            e.printStackTrace();
                        }
                    });
            });

            Util.deleteMessage(message);
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }

    public static String getSortName(String channelName) {
        String newName = channelName.replaceAll("-", " ");
        Matcher matcher = Pattern.compile("^(the|a) ").matcher(newName);
        if (matcher.find()) {
            newName = matcher.replaceFirst("");
        }
        return newName;
    }

}
