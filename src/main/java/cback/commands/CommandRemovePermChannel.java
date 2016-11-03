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
                String channelMentions = "";

                for (IChannel c : channels) {

                    List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
                    if (permChannels.contains(c.getID())) {
                        permChannels.remove(c.getID());
                        bot.getConfigManager().setConfigValue("permanentchannels", permChannels);
                    } else {
                        Util.sendMessage(message.getChannel(), c.mention() + " was not a permanent channel.");
                    }

                    channelMentions += " " + c.mention();
                }

                Util.sendMessage(message.getChannel(), "Removed " + channelMentions + " from permanent channel(s).");
            } else {
                Util.sendMessage(message.getChannel(), "Channels not found.");
            }

        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
