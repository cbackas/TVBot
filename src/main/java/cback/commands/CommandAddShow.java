package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class CommandAddShow implements Command {
    @Override
    public String getName() {
        return "addshow";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length >= 2) {
            String imdbID = args[0];
            String channelID = args[1];
            String showName = bot.getTraktManager().getShowTitle(imdbID);
            IChannel channel = client.getChannelByID(channelID);
            if (channel == null) {
                Util.sendMessage(message.getChannel(), "No channel by this ID found.");
                return;
            }
            if (showName == null) {
                Util.sendMessage(message.getChannel(), "No show by this IMDB ID found.");
                return;
            }
            bot.getDatabaseManager().insertShowData(imdbID, showName, channelID);
            Util.sendMessage(message.getChannel(), "Set channel " + channel.mention() + " for " + showName + ".");
        } else {
            Util.sendMessage(message.getChannel(), "Usage: !addshow <imdbID> <channelID>");
        }
    }
}
