package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandListPermChannels implements Command {
    @Override
    public String getName() {
        return "listpchannels";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        //Lounge Command Only
        if (guild.getID().equals("192441520178200577")) {
            if (bot.getBotAdmins().contains(message.getAuthor().getID())) {

                Util.botLog(message);
                Util.deleteMessage(message);

                List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
                StringBuilder channelMentions = new StringBuilder();
                permChannels.forEach(id -> {
                    IChannel channel = guild.getChannelByID(id);
                    if (channel != null) {
                        channelMentions.append("\n").append(channel.mention());
                    }
                });

                Util.sendMessage(message.getChannel(), "**Unmovable Channels:**\n" + channelMentions.toString());

            }
        }
    }

}
