package cback.commandsV2;

import cback.Channels;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class CommandShow extends Command {
    public CommandShow() {
        super();
        this.commandData = new CommandData("show", "Manage shows saved in the database");

        SubcommandData subcommandAdd = new SubcommandData("add", "Add a show to the database");
        SubcommandData subcommandRemove = new SubcommandData("remove", "Removes a show from the database");
        subcommandAdd.addOption(OptionType.STRING, "imdb-id", "ID for the show on IMDB.com", true);
        subcommandRemove.addOption(OptionType.STRING, "imdb-id", "ID for the show on IMDB.com", true);
        subcommandAdd.addOption(OptionType.CHANNEL, "channel", "Text channel to assign the show to, leave blank for current channel", false);

        this.commandData.addSubcommands(subcommandAdd, subcommandRemove);
    }

    @Override
    public void execute(SlashCommandEvent event) {
        MessageEmbed basicEmbed = new EmbedBuilder()
                .appendDescription("...")
                .setColor(Util.getBotColor())
                .build();

        event.replyEmbeds(basicEmbed)
                .setEphemeral(true)
                .flatMap(v -> {

                    EmbedBuilder responseEmbed = new EmbedBuilder().setColor(Util.getBotColor());
                    String imdbId = event.getOption("imdb-id").getAsString().strip();

                    if (event.getSubcommandName().equals("add")) {
                        // if channel option is null then use user's current text channel as the channel in the DB
                        TextChannel channel = event.getOption("channel") == null ? event.getTextChannel() : event.getGuild().getTextChannelById(event.getOption("channel").getAsLong());
                        Show showData = bot.getTraktManager().showSummary(imdbId);
                        String showName = showData.title;
                        String network = showData.network;

                        if (showName == null) {
                            responseEmbed.appendDescription("Error: Couldn't find a show associated with this IMDB ID");
                        } else {
                            // update database
                            bot.getDatabaseManager().getTV().insertShowData(imdbId, showName, network, channel.getId());
                            // update user
                            responseEmbed.appendDescription("Assigned **" + showName + "** to " + channel.getAsMention());
                            // logging
                            Util.getLogger().info("Assigned " + imdbId + " to " + channel.getName());
                            Util.simpleEmbed(Channels.BOTLOG_CH_ID.getChannel(), "Assigned **" + showName + "** to " + channel.getAsMention());

                            //Update airing data after new show added
                            Util.getLogger().info("Updating airing data");
                            bot.getTraktManager().updateAiringData();
                        }

                    } else if (event.getSubcommandName().equals("remove")) {
                        cback.database.tv.Show show = bot.getDatabaseManager().getTV().getShow(imdbId);
                        int entriesDeleted = bot.getDatabaseManager().getTV().deleteShow(imdbId);
                        if (show != null && entriesDeleted > 0) {
                            // update user
                            responseEmbed.appendDescription("Removed **" + show.getShowName() + "** from the database");
                            // logging
                            Util.getLogger().info("Removed " + imdbId + "from the database");
                            Util.simpleEmbed(Channels.BOTLOG_CH_ID.getChannel(), "Removed **" + show.getShowName() + "** from the database");
                        } else {
                            responseEmbed.appendDescription("There are no shows in the database with the imdb-id: **" + imdbId + "**");
                        }
                    }

                    MessageEmbed embed = responseEmbed.build();
                    return event.getHook().editOriginalEmbeds(embed);
                })
                .queue();
    }
}
