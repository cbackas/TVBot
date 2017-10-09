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

public class CommandCloseChannel implements Command {
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
        return "Closes a TV show channel and makes it all secret.";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<IChannel> channels = message.getChannelMentions();
        if (channels.size() >= 1) {
            for (IChannel c : channels) {
                if (CommandSort.getPermChannels(guild).contains(c.getCategory())) continue;
                ICategory closed = guild.getCategoryByID(355904962200469504L);
                c.changeCategory(closed);

                RequestBuffer.request(() -> {
                    try {
                        c.overrideRolePermissions(guild.getEveryoneRole(), EnumSet.noneOf(Permissions.class), EnumSet.of(Permissions.READ_MESSAGES));
                    } catch (MissingPermissionsException | DiscordException e) {
                        Util.reportHome(e);
                    }
                });
            }
        } else {
            Util.syntaxError(this, message);
        }
    }
}
