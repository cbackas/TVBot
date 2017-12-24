package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.apiutil.GuildChannelEditRequest;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        ICategory unsorted = guild.getCategoryByID(358043583355289600L);
        int count = unsorted.getChannels().size();

        List<IChannel> permChannels = getPermChannels(guild);

        //sort non permanent channels
        List<IChannel> showsChannelsSorted = guild.getChannels().stream()
                .filter(chan -> !permChannels.contains(chan))
                .sorted(Comparator.comparing(chan -> getSortName(chan.getName())))
                .collect(Collectors.toList());


        ICategory af = guild.getCategoryByID(TVBot.AF_CAT_ID);
        ICategory gl = guild.getCategoryByID(TVBot.GL_CAT_ID);
        ICategory mr = guild.getCategoryByID(TVBot.MR_CAT_ID);
        ICategory sz = guild.getCategoryByID(TVBot.SZ_CAT_ID);
        //put all the incorrectly sorted channels into their categories
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

        //apply new positions
        batchSortChannels(guild, showsChannelsSorted);

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

    private void changeCategory(IChannel channel, ICategory category) {
        if (channel.getCategory() == null || !channel.getCategory().equals(category)) {
            RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
                channel.changeCategory(category);
                return true;
            });
            future.get(); //wait for request to complete
        }
    }

    public void batchSortChannels(IGuild guild, List<IChannel> sortedChannels) {
        try {

            GuildChannelEditRequest[] edits = new GuildChannelEditRequest[sortedChannels.size()];
            for (int i = 0; i < sortedChannels.size(); i++) {
                IChannel channel = sortedChannels.get(i);
                GuildChannelEditRequest edit = new GuildChannelEditRequest.Builder().id(channel.getLongID()).position(i).build();
                edits[i] = edit;
            }

            ((DiscordClientImpl) guild.getClient())
                    .REQUESTS
                    .PATCH
                    .makeRequest
                            (
                                    DiscordEndpoints.GUILDS + guild.getStringID() + "/channels"
                                    , edits
                            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<IChannel> getPermChannels(IGuild guild) {
        ICategory staff = guild.getCategoryByID(TVBot.STAFF_CAT_ID);
        ICategory info = guild.getCategoryByID(TVBot.INFO_CAT_ID);
        ICategory disc = guild.getCategoryByID(TVBot.DISCUSSION_CAT_ID);
        ICategory fun = guild.getCategoryByID(TVBot.FUN_CAT_ID);
        ICategory closed = guild.getCategoryByID(TVBot.CLOSED_CAT_ID);

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

        return permChannels;
    }
}
