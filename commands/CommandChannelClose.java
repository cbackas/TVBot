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
import java.util.EnumSet;
import java.util.List;

public class CommandChannelClose implements Command {
    @Override
    public String getName() {
        return "closechannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("close");
    }

    @Override
    public String getSyntax() {
        return "closechannel #channel";
    }

    @Override
    public String getDescription() {
        return "Closes a TV show channel and makes it all secret. If you don't specify a channel, it will just close";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<IChannel> channels = message.getChannelMentions();
        if (channels.size() == 0 && args[0].equalsIgnoreCase("here")) {
            channels.add(message.getChannel());
        }

        if (channels.size() >= 1) {
            String mentions = closeChannels(guild, channels);

            String text = "Closed " + channels.size() + " channel(s).\n" + mentions;
            Util.simpleEmbed(message.getChannel(), text);
            Util.sendLog(message, text);
        } else {
            Util.syntaxError(this, message);
        }
    }

    private String closeChannels(IGuild guild, List<IChannel> channels) {
        StringBuilder mentions = new StringBuilder();
        for (IChannel c : channels) {
            if (CommandSort.getPermChannels(guild).contains(c.getCategory())) continue;
            ICategory closed = guild.getCategoryByID(355904962200469504L);
            c.changeCategory(closed);

            try {
                RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
                    c.overrideRolePermissions(guild.getEveryoneRole(), EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.READ_MESSAGES));
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
