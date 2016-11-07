package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

import java.util.List;

public class CommandAddPermChannel implements Command {
    @Override
    public String getName() {
        return "addpchannel";
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
                    if (!permChannels.contains(c.getID())) {
                        permChannels.add(c.getID());
                        bot.getConfigManager().setConfigValue("permanentchannels", permChannels);

                        channelMentions.append(" ").append(c.mention());
                    } else {
                        Util.sendMessage(message.getChannel(), c.mention() + " already exists.");
                    }

                }

                Util.sendMessage(message.getChannel(), "Set " + channelMentions.toString() + " as permanent channel(s).");
            } else {
                Util.sendMessage(message.getChannel(), "Channels not found.");
            }

            Util.botLog(message);
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
