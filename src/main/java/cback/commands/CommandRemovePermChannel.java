package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandRemovePermChannel implements Command {
    @Override
    public String getName() {
        return "removepchannel";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (bot.getBotAdmins().contains(message.getAuthor().getID())) {

            Util.deleteMessage(message);

            List<IChannel> channels = message.getChannelMentions();
            if (channels.size() >= 1) {
                StringBuilder channelMentions = new StringBuilder();

                for (IChannel c : channels) {

                    List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
                    if (permChannels.contains(c.getID())) {
                        permChannels.remove(c.getID());
                        bot.getConfigManager().setConfigValue("permanentchannels", permChannels);

                        channelMentions.append(" ").append(c.mention());
                    } else {
                        Util.sendMessage(message.getChannel(), c.mention() + " was not a permanent channel.");
                    }

                }

                Util.sendMessage(message.getChannel(), "Removed " + channelMentions.toString() + " from permanent channel(s).");
            } else {
                Util.sendMessage(message.getChannel(), "Channels not found.");
            }

        }
    }

}
