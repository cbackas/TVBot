package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Status;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandShowAdd implements Command {
    @Override
    public String getName() {
        return "addshow";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "addshow [imdbID] [here|channelID]";
    }

    @Override
    public String getDescription() {
        return "Associates a channel and a show in the spooky bot database :o";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length >= 2) {
            String imdbID = args[0];
            String channelID = args[1];
            if (channelID.equalsIgnoreCase("here")) channelID = message.getChannel().getStringID();
            Show showData = bot.getTraktManager().showSummary(imdbID);
            String showName = showData.title;
            IChannel channel = client.getChannelByID(Long.parseLong(channelID));
            if (channel == null) {
                Util.simpleEmbed(message.getChannel(), "Error: No channel by this ID found.");
                return;
            }
            if (showName == null) {
                Util.simpleEmbed(message.getChannel(), "Error: Couldn't find a show associated with this IMDB ID");
                return;
            }
            bot.getDatabaseManager().getTV().insertShowData(imdbID, showName, channelID);
            Util.simpleEmbed(message.getChannel(), "Set channel " + channel.mention() + " for " + showName + ".");
            System.out.println("@" + message.getAuthor().getName() + " added show " + showName);
            Util.simpleEmbed(client.getChannelByID(TVBot.BOTLOG_CH_ID), showName + " assigned to " + channel.getName());
            //Update airing data after new show added
            bot.getTraktManager().updateAiringData();

            //Builds a show embed thing
            String title = showData.title + " (" + Integer.toString(showData.year) + ") ";
            String overview = showData.overview;
            String airs = (showData.status == com.uwetrottmann.trakt5.enums.Status.RETURNING || showData.status == Status.IN_PRODUCTION)
                    ? showData.airs.day + " at " + Util.to12Hour(showData.airs.time) + " EST on " + showData.network : "Ended";
            String premier = new SimpleDateFormat("MMM dd, yyyy").format(new Date(showData.first_aired.toInstant().toEpochMilli()));
            String runtime = Integer.toString(showData.runtime);
            String country = showData.country + " - " + showData.language;
            String homepage = "<https://trakt.tv/shows/" + showData.ids.slug + ">\n<http://www.imdb.com/title/" + showData.ids.imdb + ">";

            try {
                overview = guild.getChannelByID(Long.parseLong(bot.getDatabaseManager().getTV().getShow(showData.ids.imdb).getChannelID())).mention() + "\n" + overview;
            } catch (Exception ignored) {
            }

            EmbedBuilder embed = new EmbedBuilder();

            embed
                    .withTitle(title)
                    .withDescription(overview)
                    .appendField("References:", homepage, false)
                    .appendField("AIRS:", airs, true)
                    .appendField("RUNTIME:", runtime, true)
                    .appendField("PREMIERED:", premier, true)
                    .appendField("COUNTRY:", country.toUpperCase(), true)
                    .appendField("GENRES:", String.join(", ", showData.genres), true)
                    .withColor(Util.getBotColor());

            Util.sendEmbed(message.getChannel(), embed.build());
        } else {
            Util.syntaxError(this, message);
        }
    }

}
