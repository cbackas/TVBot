package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
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
    public String getSyntax() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<IChannel> channels = message.getChannelMentions();
        if (channels.size() >= 1) {
            StringBuilder channelMentions = new StringBuilder();

            for (IChannel c : channels) {

                List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
                if (!permChannels.contains(c.getStringID())) {
                    permChannels.add(c.getStringID());
                    bot.getConfigManager().setConfigValue("permanentchannels", permChannels);

                    channelMentions.append(" ").append(c.mention());
                } else {
                    Util.simpleEmbed(message.getChannel(), c.mention() + " already exists.");
                }

            }

            Util.simpleEmbed(message.getChannel(), "Set " + channelMentions.toString() + " as permanent channel(s).");
        } else {
            Util.simpleEmbed(message.getChannel(), "Channels not found.");
        }

        Util.deleteMessage(message);
    }

}
