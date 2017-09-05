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
    public String getSyntax() {
        return "removepchannel #channel";
    }

    @Override
    public String getDescription() {
        return "Removes a channel from the permanent channel list Tip: mentioning more than one channels will remove more than one channel!";
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
                if (permChannels.contains(c.getStringID())) {
                    permChannels.remove(c.getStringID());
                    bot.getConfigManager().setConfigValue("permanentchannels", permChannels);

                    channelMentions.append(" ").append(c.mention());
                } else {
                    Util.simpleEmbed(message.getChannel(), c.mention() + " was not a permanent channel.");
                }

            }

            Util.simpleEmbed(message.getChannel(), "Removed " + channelMentions.toString() + " from permanent channel(s).");
        } else {
            Util.syntaxError(this, message);
        }
        Util.deleteMessage(message);
    }

}
