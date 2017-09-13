package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
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
            String showNetwork = showData.network;
            IChannel channel = client.getChannelByID(Long.parseLong(channelID));
            if (channel == null) {
                Util.simpleEmbed(message.getChannel(), "No channel by this ID found.");
                return;
            }
            if (showName == null) {
                Util.simpleEmbed(message.getChannel(), "No show by this IMDB ID found.");
                return;
            }
            if (showNetwork.equalsIgnoreCase("netflix")) {
                Util.simpleEmbed(message.getChannel(), "Netflix show detected - import aborted");
                return;
            }
            bot.getDatabaseManager().getTV().insertShowData(imdbID, showName, channelID);
            Util.simpleEmbed(message.getChannel(), "Set channel " + channel.mention() + " for " + showName + ".");
            System.out.println("@" + message.getAuthor().getName() + " added show " + showName);
            Util.simpleEmbed(client.getChannelByID(TVBot.BOTLOG_CH_ID), showName + " assigned to " + channel.getName());
            //Update airing data after new show added
            bot.getTraktManager().updateAiringData();
        } else {
            Util.syntaxError(this, message);
        }
    }

}
