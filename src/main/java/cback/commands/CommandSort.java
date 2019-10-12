package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.order.ChannelOrderAction;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandSort extends Command {

    private TVBot bot;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    public CommandSort() {
        this.bot = TVBot.getInstance();
        this.name = "sort";
        this.arguments = "sort";
        this.help = "Alphabetically sorts the channels";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        Util.simpleEmbed(commandEvent.getTextChannel(), "Lets get sorting!");

        var unsortedCat = commandEvent.getGuild().getCategoryById(358043583355289600L);
        int unsortedCount = unsortedCat.getChannels().size();

        List<GuildChannel> permChannels = Util.getPermChannels(commandEvent.getGuild());

        //sort non permanent channels
        List<GuildChannel> showChannelsSorted = commandEvent.getGuild().getChannels().stream()
                .filter(chan -> !permChannels.contains(chan))
                .filter(channel -> channel.getType() == ChannelType.TEXT)
                .sorted(Comparator.comparing(chan -> getSortName(chan.getName())))
                .collect(Collectors.toList());

        //alphabetic categories
        var af = commandEvent.getGuild().getCategoryById(TVBot.AF_CAT_ID);
        var gl = commandEvent.getGuild().getCategoryById(TVBot.GL_CAT_ID);
        var mr = commandEvent.getGuild().getCategoryById(TVBot.MR_CAT_ID);
        var sz = commandEvent.getGuild().getCategoryById(TVBot.SZ_CAT_ID);

        //put all the incorrectly sorted channels into their categories
        for (GuildChannel c : showChannelsSorted) {

            //do not move channels from the closed category!
            if (c.getParent() != null && c.getParent().getIdLong() == TVBot.CLOSED_CAT_ID) continue;

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

        //create order action and apply new positions
        var orderAction = batchSortChannels(commandEvent.getGuild(), showChannelsSorted);
        //queue up the sort action
        if (orderAction != null) {

            orderAction.queue(successReturn -> {
                Util.simpleEmbed(commandEvent.getTextChannel(), "All done! " + unsortedCount + " channels moved, " + showChannelsSorted.size() + " channels sorted.");
                Util.getLogger().info("Successfully sorted channels");
            }, failureReturn -> {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Something went wrong...error during sort action");
                Util.getLogger().info("Failed to sort channels");
                failureReturn.printStackTrace();
            });

        } else {
            Util.simpleEmbed(commandEvent.getTextChannel(), "Something went wrong...error during sort action");
        }


    }

    public static String getSortName(String channelName) {
        String newName = channelName.replaceAll("-", " ");
        Matcher matcher = Pattern.compile("^(the|a) ").matcher(newName);
        if (matcher.find()) {
            newName = matcher.replaceFirst("");
        }
        return newName;
    }

    private void changeCategory(GuildChannel channel, net.dv8tion.jda.api.entities.Category category) {
        if (channel.getParent() == null || !channel.getParent().equals(category)) {
            channel.getManager().setParent(category).queue();
        }
    }

    public ChannelOrderAction batchSortChannels(Guild guild, List<GuildChannel> sortedChannels) {
        try {

            //ChannelOrderAction instance to bulk sort channels
            var channelOrderAction = guild.modifyTextChannelPositions();

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

            return channelOrderAction;


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
