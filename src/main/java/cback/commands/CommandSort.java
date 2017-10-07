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
        Util.simpleEmbed(message.getChannel(), "Lets get sorting!");

        this.count = 0;

        ICategory staff = guild.getCategoryByID(355901035597922304l);
        ICategory info = guild.getCategoryByID(355910636464504832l);
        ICategory disc = guild.getCategoryByID(355910667812995084l);
        ICategory fun = guild.getCategoryByID(358679449451102210l);
        ICategory af = guild.getCategoryByID(358038418208587785l);
        ICategory gl = guild.getCategoryByID(358038474894606346l);
        ICategory mr = guild.getCategoryByID(358038505244327937l);
        ICategory sz = guild.getCategoryByID(358038532780195840l);
        ICategory closed = guild.getCategoryByID(355904962200469504l);

        List<ICategory> permCategories = new ArrayList<>();
        permCategories.add(staff);
        permCategories.add(info);
        permCategories.add(disc);
        permCategories.add(fun);
        permCategories.add(closed);

        List<IChannel> permChannels = new ArrayList<>();
        for (ICategory cat : permCategories) {
            permChannels.addAll(cat.getChannels());
        }

        //sort non permanent channels
        List<IChannel> showsChannelsSorted = guild.getChannels().stream()
                .filter(chan -> !permChannels.contains(chan))
                .sorted(Comparator.comparing(chan -> getSortName(chan.getName())))
                .collect(Collectors.toList());


        //apply new positions
        IntStream.range(0, showsChannelsSorted.size()).forEach(position -> {
            IChannel channel = showsChannelsSorted.get(position);
            if (!(channel.getPosition() == position)) { //don't sort if position is already correct
                RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
                    try {
                        channel.changePosition(position);
                        count++;
                        return true;
                    } catch (DiscordException e) {
                        Util.reportHome(message, e);
                    } catch (MissingPermissionsException e) {
                        Util.reportHome(message, e);
                    }
                    return false;
                });
                future.get(); //wait for request to complete
            }
        });

        //put all the unsorted channels into their categories
        for (IChannel c : guild.getChannels()) {
            if (!permChannels.contains(c)) {
                String channelName = getSortName(c.getName());
                String alph = "abcdefghijklmnopqrstuvwxyz";
                char firstLetter = channelName.toLowerCase().charAt(0);
                int index = alph.indexOf(firstLetter) + 1;
                if (index <= 6) {
                    changeCategory(c, af);
                } else if (index > 6 && index <= 12) {
                    changeCategory(c, gl);
                } else if (index > 12 && index <= 18) {
                    changeCategory(c, mr);
                } else if (index > 18 && index <= 26) {
                    changeCategory(c, sz);
                }
            }
        }

        Util.simpleEmbed(message.getChannel(), "All done! " + count + " channel(s) sorted.");
    }

    public static String getSortName(String channelName) {
        String newName = channelName.replaceAll("-", " ");
        Matcher matcher = Pattern.compile("^(the|a) ").matcher(newName);
        if (matcher.find()) {
            newName = matcher.replaceFirst("");
        }
        return newName;
    }

    private int count = 0;

    private void changeCategory(IChannel channel, ICategory category) {
        RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
            channel.changeCategory(category);
            return true;
        });
        future.get(); //wait for request to complete
    }
}
