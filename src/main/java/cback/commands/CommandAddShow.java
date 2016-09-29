package cback.commands;

import cback.TVbot;
import cback.TraktHandler;
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
    public void execute(TVbot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length >= 2) {
            String imdbID = args[0];
            String channelID = args[1];
            String showName = bot.getTraktHandler().getShowTitle(imdbID);
            IChannel channel = client.getChannelByID(channelID);
            if (channel == null) {
                //CHANNEL NOT FOUND
                return;
            }
            if (showName == null) {
                //SHOW NOT FOUND
                return;
            }
            bot.getDatabaseManager().insertShowData(imdbID, showName, channelID);
            //SUCCESS ADDED OMG WOW
        } else {
            //Usage
        }
    }
}
