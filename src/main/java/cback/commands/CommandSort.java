package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.apiutil.GuildChannelEditRequest;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.requests.Request;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandSort extends Command {

    private TVBot bot;

    public CommandSort(TVBot bot) {
        this.bot = bot;
        this.name = "sort";
        this.arguments = "sort";
        this.help = "Alphabetically sorts the channels";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        Util.simpleEmbed(commandEvent.getChannel(), "Lets get sorting!");

        net.dv8tion.jda.core.entities.Category unsorted = commandEvent.getGuild().getCategoryById(358043583355289600L);
        int count = unsorted.getChannels().size();

        List<Channel> permChannels = getPermChannels(commandEvent.getGuild());

        //sort non permanent channels
        List<Channel> showChannelsSorted = commandEvent.getGuild().getChannels().stream()
                .filter(chan -> !permChannels.contains(chan))
                .sorted(Comparator.comparing(chan -> getSortName(chan.getName())))
                .collect(Collectors.toList());

        net.dv8tion.jda.core.entities.Category af = commandEvent.getGuild().getCategoryById(TVBot.AF_CAT_ID);
        net.dv8tion.jda.core.entities.Category gl = commandEvent.getGuild().getCategoryById(TVBot.GL_CAT_ID);
        net.dv8tion.jda.core.entities.Category mr = commandEvent.getGuild().getCategoryById(TVBot.MR_CAT_ID);
        net.dv8tion.jda.core.entities.Category sz = commandEvent.getGuild().getCategoryById(TVBot.SZ_CAT_ID);

        //put all the incorrectly sorted channels into their categories
        for(Channel c : commandEvent.getGuild().getChannels()) {
            if(!permChannels.contains(c)) {
                String channelName = getSortName(c.getName());
                String alph = "abcdefghijklmnopqrstuvwxyz";
                char firstLetter = channelName.toLowerCase().charAt(0);
                int index = alph.indexOf(firstLetter) + 1;
                if(index <= 6) {
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
        batchSortChannels(commandEvent.getGuild(), showChannelsSorted);

        Util.simpleEmbed(commandEvent.getChannel(), "All done!" + count + " channe;(s) sorted.");
    }

    public static String getSortName(String channelName) {
        String newName = channelName.replaceAll("-", " ");
        Matcher matcher = Pattern.compile("^(the|a) ").matcher(newName);
        if (matcher.find()) {
            newName = matcher.replaceFirst("");
        }
        return newName;
    }

    private void changeCategory(Channel channel, net.dv8tion.jda.core.entities.Category category) {
        if (channel.getParent() == null || !channel.getParent().equals(category)) {
                channel.getManager().setParent(category).queue();
        }
    }

    public void batchSortChannels(Guild guild, List<Channel> sortedChannels) {
        try {

            GuildChannelEditRequest[] edits = new GuildChannelEditRequest[sortedChannels.size()];
            for (int i = 0; i < sortedChannels.size(); i++) {
                Channel channel = sortedChannels.get(i);
                GuildChannelEditRequest edit = new GuildChannelEditRequest.Builder().id(channel.getIdLong()).position(i).build();
                edits[i] = edit;
            }

            ((Request) guild.getJDA())
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

    public static List<Channel> getPermChannels(Guild guild) {
        net.dv8tion.jda.core.entities.Category staff = guild.getCategoryById(TVBot.STAFF_CAT_ID);
        net.dv8tion.jda.core.entities.Category info = guild.getCategoryById(TVBot.INFO_CAT_ID);
        net.dv8tion.jda.core.entities.Category disc = guild.getCategoryById(TVBot.DISCUSSION_CAT_ID);
        net.dv8tion.jda.core.entities.Category fun = guild.getCategoryById(TVBot.FUN_CAT_ID);
        net.dv8tion.jda.core.entities.Category closed = guild.getCategoryById(TVBot.CLOSED_CAT_ID);

        List<net.dv8tion.jda.core.entities.Category> permCategories = new ArrayList<>();
        permCategories.add(staff);
        permCategories.add(info);
        permCategories.add(disc);
        permCategories.add(fun);
        permCategories.add(closed);

        List<Channel> permChannels = new ArrayList<>();
        for (net.dv8tion.jda.core.entities.Category cat : permCategories) {
            permChannels.addAll(cat.getChannels());
        }

        return permChannels;
    }
}
