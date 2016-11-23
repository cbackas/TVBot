package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

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
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (bot.getBotAdmins().contains(message.getAuthor().getID())) {
            if (args.length >= 2) {
                String imdbID = args[0];
                String channelID = args[1];
                if (channelID.equalsIgnoreCase("here")) channelID = message.getChannel().getID();
                Show showData = bot.getTraktManager().showSummary(imdbID);
                String showName = showData.title;
                String showNetwork = showData.network;
                IChannel channel = client.getChannelByID(channelID);
                if (channel == null) {
                    Util.sendMessage(message.getChannel(), "No channel by this ID found.");
                    return;
                }
                if (showName == null) {
                    Util.sendMessage(message.getChannel(), "No show by this IMDB ID found.");
                    return;
                }
                if (showNetwork.equalsIgnoreCase("netflix")) {
                    Util.sendMessage(message.getChannel(), "Netflix show detected - import aborted");
                    return;
                }
                bot.getDatabaseManager().getTV().insertShowData(imdbID, showName, channelID);
                Util.sendMessage(message.getChannel(), "Set channel " + channel.mention() + " for " + showName + ".");
                System.out.println("@" + message.getAuthor().getName() + " added show " + showName);
                //Update airing data after new show added
                bot.getTraktManager().updateAiringData();
            } else {
                Util.sendMessage(message.getChannel(), "Usage: !addshow <imdbID> <here|channelID>");
            }
            Util.botLog(message);
        }
    }

}
