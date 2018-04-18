package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
import java.util.List;

public class CommandChannelDelete implements Command {
    @Override
    public String getName() {
        return "deletechannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("removechannel");
    }

    @Override
    public String getSyntax() {
        return "deletechannel #channel";
    }

    @Override
    public String getDescription() {
        return "Deletes any and all mentioned channels you provide";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<IChannel> channels = message.getChannelMentions();
        IChannel here = message.getChannel();
        if (channels.size() == 0 && args[0].equalsIgnoreCase("here")) {
            channels.add(here);
        }

        if (channels.size() >= 1) {
            String mentions = deleteChannels(channels);

            String text = "Deleted " + channels.size() + " channel(s).\n" + mentions;
            if (!channels.contains(here)) {
                Util.simpleEmbed(message.getChannel(), text);
            }

            Util.sendLog(message, text);
        } else {
            Util.syntaxError(this, message);
        }
    }

    private String deleteChannels(List<IChannel> channels) {
        StringBuilder mentions = new StringBuilder();
        for (IChannel c : channels) {
            try {
                RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
                    c.delete();
                    return true;
                });
                future.get();
                mentions.append("#" + c.getName() + " ");
            } catch (MissingPermissionsException | DiscordException e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }

}
