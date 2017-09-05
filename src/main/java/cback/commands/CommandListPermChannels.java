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
    public String getSyntax() {
        return "listpchannels";
    }

    @Override
    public String getDescription() {
        return "Shows a list of all the channels currently tagged as permanent (not to be sorted)";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
        StringBuilder channelMentions = new StringBuilder();

        permChannels.forEach(id -> {
            IChannel channel = guild.getChannelByID(Long.parseLong(id));
            if (channel != null) {
                channelMentions.append("\n").append(channel.mention());
            }
        });

        Util.simpleEmbed(message.getChannel(), "**Unmovable Channels:**\n" + channelMentions.toString());

        Util.deleteMessage(message);
    }


}
