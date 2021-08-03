package cback.commandsV2;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.order.ChannelOrderAction;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandSort extends Command {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    private int sortedChannels = 0;

    public CommandSort() {
        super();

        this.commandData = new CommandData("sort", "Sorts text channels into alphebetic categories")
                .setDefaultEnabled(false);
        this.commandPrivileges.add(new CommandPrivilege(CommandPrivilege.Type.ROLE, true, TVRoles.ADMIN.id));
    }

    @Override
    public void execute(SlashCommandEvent event) {
        MessageEmbed startingEmbed = new EmbedBuilder()
                .setDescription("Sort requested ...")
                .setColor(Util.getBotColor())
                .build();

        event.replyEmbeds(startingEmbed)
                .setEphemeral(true)
                .flatMap(v -> {
                    List<Long> permCategories = Arrays.asList(TVBot.STAFF_CAT_ID, TVBot.INFO_CAT_ID, TVBot.DISCUSSION_CAT_ID, TVBot.FUN_CAT_ID, TVBot.CARDS_CAT_ID, TVBot.NEW_CAT_ID, TVBot.CLOSED_CAT_ID);

                    // sort non permanent text channels
                    List<GuildChannel> showChannelsSorted = event.getGuild().getChannels().stream()
                            .filter(c -> c.getParent() != null && !permCategories.contains(c.getParent().getIdLong()))
                            .filter(c -> c.getType() == ChannelType.TEXT)
                            .sorted(Comparator.comparing(c -> getSortName(c.getName())))
                            .collect(Collectors.toList());

                    // alphabetic categories
                    var af = event.getGuild().getCategoryById(TVBot.AF_CAT_ID);
                    var gl = event.getGuild().getCategoryById(TVBot.GL_CAT_ID);
                    var mr = event.getGuild().getCategoryById(TVBot.MR_CAT_ID);
                    var sz = event.getGuild().getCategoryById(TVBot.SZ_CAT_ID);

                    // put all the incorrectly sorted channels into their categories
                    for (GuildChannel c : showChannelsSorted) {
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

                    // create order action and apply new positions
                    var orderAction = batchSortChannels(event.getGuild(), showChannelsSorted);

                    EmbedBuilder embedBuilder = new EmbedBuilder();

                    if (orderAction == null) return null;

                    embedBuilder
                            .setDescription("Sorted channel order is being submitted to Discord...")
                            .setColor(Color.YELLOW);

                    event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();

                    this.sortedChannels = showChannelsSorted.size();
                    return orderAction;
                })
                .queue(successReturn -> {
                    MessageEmbed embed = new EmbedBuilder()
                            .setDescription("All done! " + this.sortedChannels + " channels are now sorted.")
                            .setColor(Color.GREEN)
                            .build();

                    event.getHook().editOriginalEmbeds(embed).queue();
                    Util.getLogger().info("Successfully sorted channels");
                }, failureReturn -> {
                    MessageEmbed embed = new EmbedBuilder()
                            .setDescription("Something went wrong... error during sort action")
                            .setColor(Color.RED)
                            .build();

                    event.getHook().editOriginalEmbeds(embed).queue();
                    Util.getLogger().info("Failed to sort channels");
                    failureReturn.printStackTrace();
                });
    }

    private static String getSortName(String channelName) {
        String newName = channelName.replaceAll("-", " ");
        Matcher matcher = Pattern.compile("^(the|a) ").matcher(newName);
        if (matcher.find()) {
            newName = matcher.replaceFirst("");
        }
        return newName;
    }

    private void changeCategory(GuildChannel channel, net.dv8tion.jda.api.entities.Category category) {
        if (channel.getParent() == null || !channel.getParent().equals(category)) {
            Util.getLogger().info("Moving channel " + channel.getName() + " from " + channel.getParent().getName() + " to " + category.getName());
            channel.getManager().setParent(category).queue();
        }
    }

    private ChannelOrderAction batchSortChannels(Guild guild, List<GuildChannel> sortedChannels) {
        try {

            // ChannelOrderAction instance to bulk sort channels
            var channelOrderAction = guild.modifyTextChannelPositions();

            // iterate sorted channels and set their positions accordingly
            for (int i = 0; i < sortedChannels.size(); i++) {
                // we can only sort TextChannel instances
                if (sortedChannels.get(i) instanceof TextChannel) {
                    var sortedChannel = (TextChannel) sortedChannels.get(i);
                    // select the channel
                    channelOrderAction.selectPosition(sortedChannel);
                    // set its position to its position in the sorted array
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
