package cback.commandsDepr;

import cback.Channels;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.uwetrottmann.trakt5.entities.Show;
import net.dv8tion.jda.api.entities.TextChannel;

public class CommandShowAdd extends Command {

    private TVBot bot;

    public CommandShowAdd() {
        this.bot = TVBot.getInstance();
        this.name = "addshow";
        this.arguments = "addshow [imdbID] [here|channelID]";
        this.help = "Associates a channel and a show in the spooky bot database :o";
        this.requiredRole = TVRoles.MOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        if (args.length >= 2) {
            String imdbID = args[0];
            String channelID = args[1];
            if (channelID.equalsIgnoreCase("here")) channelID = commandEvent.getChannel().getId();

            Show showData = bot.getTraktManager().showSummary(imdbID);
            String showName = showData.title;
            String network = showData.network;
            TextChannel channel = commandEvent.getGuild().getTextChannelById(Long.parseLong(channelID));

            if (channel == null) {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Error: No channel by this ID found.");
                return;
            }
            if (showName == null) {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Error: Couldn't find a show associated with this IMDB ID");
                return;
            }

            bot.getDatabaseManager().getTV().insertShowData(imdbID, showName, network, channelID);
            Util.simpleEmbed(commandEvent.getTextChannel(), "Set channel " + channel.getAsMention() + " for " + showName + ".");
            Util.getLogger().info("@" + commandEvent.getAuthor().getName() + " added show " + showName);
            Util.simpleEmbed(Channels.BOTLOG_CH_ID.getChannel(), showName + " assigned to " + channel.getName());

            //Update airing data after new show added
            Util.getLogger().info("UPDATING AIRING DATA");
            bot.getTraktManager().updateAiringData();

            //Builds a show embed ting
            //TODO does it need to send this
//            String title = showData.title + " (" + Integer.toString(showData.year) + ") ";
//            String overview = showData.overview;
//            String airs = (showData.status == Status.RETURNING || showData.status == Status.IN_PRODUCTION)
//                    ? showData.airs.day + " at " + Util.to12Hour(showData.airs.time) + " EST on " + showData.network : "Ended";
//            String premier = new SimpleDateFormat("MMM dd, yyyy").format(new Date(showData.first_aired.toInstant().toEpochMilli()));
//            String runtime = Integer.toString(showData.runtime);
//            String country = showData.country + " - " + showData.language;
//            String homepage = "<https://trakt.tv/shows/" + showData.ids.slug + ">\n<http://www.imdb.com/title/" + showData.ids.imdb + ">";
//
//            try {
//                overview = commandEvent.getGuild().getTextChannelById(Long.parseLong(bot.getDatabaseManager().getTV().getShow(showData.ids.imdb).getChannelID())).getAsMention() + "\n" + overview;
//            } catch (Exception ignore) {
//            }
//
//            EmbedBuilder embed = new EmbedBuilder();
//
//            embed
//                    .setTitle(title)
//                    .setDescription(overview)
//                    .addField("References:", homepage, false)
//                    .addField("AIRS:", airs, true)
//                    .addField("RUNTIME:", runtime, true)
//                    .addField("PREMIERED:", premier, true)
//                    .addField("COUNTRY:", country.toUpperCase(), true)
//                    .addField("GENRES:", String.join(", ", showData.genres), true)
//                    .setColor(Util.getBotColor());
//
//            Util.sendEmbed(commandEvent.getChannel(), embed.build());
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }
}