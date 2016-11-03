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
        if (bot.getBotAdmins().contains(message.getAuthor().getID())) {

            Util.deleteMessage(message);

            List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
            String channelMentions = "";
            for (String c : permChannels) {

                IChannel channel = guild.getChannelByID(c);

                channelMentions += "\n" + channel.mention();
            }

            Util.sendMessage(message.getChannel(), "**Unmovable Channels:**\n" + channelMentions);

        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
