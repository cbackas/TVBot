package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandSort extends Command {

    private TVBot bot;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

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
        List<Channel> showChannelsSorted = commandEvent.getGuild().getChannels().stream().filter(chan -> !permChannels.contains(chan)).sorted(Comparator.comparing(chan -> getSortName(chan.getName()))).collect(Collectors.toList());

        net.dv8tion.jda.core.entities.Category af = commandEvent.getGuild().getCategoryById(TVBot.AF_CAT_ID);
        net.dv8tion.jda.core.entities.Category gl = commandEvent.getGuild().getCategoryById(TVBot.GL_CAT_ID);
        net.dv8tion.jda.core.entities.Category mr = commandEvent.getGuild().getCategoryById(TVBot.MR_CAT_ID);
        net.dv8tion.jda.core.entities.Category sz = commandEvent.getGuild().getCategoryById(TVBot.SZ_CAT_ID);

        //put all the incorrectly sorted channels into their categories
        for (Channel c : commandEvent.getGuild().getChannels()) {
            if (!permChannels.contains(c)) {
                String channelName = getSortName(c.getName());
                char firstLetter = channelName.toLowerCase().charAt(0);
                int index = ALPHABET.indexOf(firstLetter) + 1;
                if (index <= 6) {
                    changeCategory(c, af); // A-F
                } else if (index > 6 && index <= 12) {
                    changeCategory(c, gl); // G-L
                } else if (index > 12 && index <= 18) {
                    changeCategory(c, mr); // M-R
                } else if (index > 18 && index <= 26) {
                    changeCategory(c, sz); // S-Z
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

            //ChannelOrderAction instance to bulk sort channels
            var channelOrderAction = guild.getController().modifyTextChannelPositions();

            //iterate sorted channels and set their positions accordingly
            for (int i = 0; i < sortedChannels.size(); i++) {
                //we can only sort TextChannel instances
                if (sortedChannels.get(i) instanceof TextChannel) {
                    var sortedChannel = (TextChannel) sortedChannels.get(i);
                    //select the channel
                    channelOrderAction.selectPosition(sortedChannel);
                    //set its position to its position in the sorted array
                    channelOrderAction.moveTo(i);
                }
            }

            //queue up the sort
            channelOrderAction.queue(successReturn -> {
                //TODO on success - send message?
                System.out.println("Successfully sorted channels");
            }, failureReturn -> {
                //TODO on failure - send message?
                System.out.println("Failed to sort channels");
                failureReturn.printStackTrace();
            });

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
