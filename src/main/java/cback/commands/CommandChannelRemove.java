package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
import java.util.List;

public class CommandChannelRemove implements Command {
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
        List<IChannel> mentionsC = message.getChannelMentions();
        if (!mentionsC.isEmpty()) {
            for (IChannel c : mentionsC) {
                RequestBuffer.request(() -> {
                    try {
                        c.delete();
                        Util.sendLog(message, "Deleted " + c.getName() + " channel.");
                    } catch (DiscordException | MissingPermissionsException e) {
                        Util.reportHome(message, e);
                    }
                });
            }
        } else if (args[0].equalsIgnoreCase("here")) {
            IChannel here = message.getChannel();

            RequestBuffer.request(() -> {
                try {
                    here.delete();
                } catch (DiscordException | MissingPermissionsException e) {
                    Util.reportHome(message, e);
                }
            });
        } else {
            Util.simpleEmbed(message.getChannel(), "Error! Couldn't find channel to delete.");
        }

        Util.deleteMessage(message);
    }

}
