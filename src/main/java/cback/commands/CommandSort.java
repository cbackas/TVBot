package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    public String getSyntax() {
        return "sort";
    }

    @Override
    public String getDescription() {
        return "Alphabetically sorts the channels";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        /*Util.simpleEmbed(message.getChannel(), "Sorting time! Here we go.");
        //permanent channels sorted by position to keep on top or bottom
        List<IChannel> permChannels = bot.getConfigManager().getConfigArray("permanentchannels").stream()
                .map(id -> guild.getChannelByID(Long.parseLong(id)))
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
                        Util.reportHome(message, e);
                    } catch (MissingPermissionsException e) {
                        Util.reportHome(message, e);
                    }
                });
        });

        Util.simpleEmbed(message.getChannel(), "Sorting complete!");
        Util.deleteMessage(message);
        */

        ICategory staff = guild.getCategoryByID(355901035597922304l);
        ICategory info = guild.getCategoryByID(355910636464504832l);
        ICategory disc = guild.getCategoryByID(355910667812995084l);
        ICategory fun = guild.getCategoryByID(358679449451102210l);
        ICategory af = guild.getCategoryByID(358038418208587785l);
        ICategory gl = guild.getCategoryByID(358038474894606346l);
        ICategory mr = guild.getCategoryByID(358038505244327937l);
        ICategory sz = guild.getCategoryByID(358038532780195840l);
        ICategory unsorted = guild.getCategoryByID(358043583355289600l);
        ICategory closed = guild.getCategoryByID(355904962200469504l);

        List<IChannel> permChannels = new ArrayList<>();
        permChannels.addAll(staff.getChannels());
        permChannels.addAll(info.getChannels());
        permChannels.addAll(disc.getChannels());
        permChannels.addAll(fun.getChannels());
        permChannels.addAll(closed.getChannels());

        //permanent channels sorted by position to keep on top or bottom
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

        //put all the unsorted channels into their categories
        if (!unsorted.getChannels().isEmpty()) {
            for (IChannel c : unsorted.getChannels()) {
                String channelName = getSortName(c.getName());
                String alph = "abcdefghijklmnopqrstuvwxyz";
                char firstLetter = channelName.charAt(0);
                int index = alph.indexOf(firstLetter) + 1;
                if (index <= 6) {
                    c.changeCategory(af);
                } else if (index > 6 && index <= 12) {
                    c.changeCategory(gl);
                } else if (index > 12 && index <= 18) {
                    c.changeCategory(mr);
                } else if (index > 18 && index <= 26) {
                    c.changeCategory(sz);
                }
            }
        }

        //sort non permanent channels
        List<IChannel> showsChannelsSorted = guild.getChannels().stream()
                .filter(chan -> !permChannels.contains(chan))
                .sorted(Comparator.comparing(chan -> getSortName(chan.getName())))
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
                        Util.reportHome(message, e);
                    } catch (MissingPermissionsException e) {
                        Util.reportHome(message, e);
                    }
                });
        });


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
