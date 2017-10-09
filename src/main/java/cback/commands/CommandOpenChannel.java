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

public class CommandOpenChannel implements Command {
    @Override
    public String getName() {
        return "openchannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("open");
    }

    @Override
    public String getSyntax() {
        return "openchannel #channel";
    }

    @Override
    public String getDescription() {
        return "Moves desired channels from the closed category and opens them up to the world.";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<IChannel> channels = message.getChannelMentions();
        if (channels.size() >= 1) {
            StringBuilder mentions = new StringBuilder();
            for (IChannel c : channels) {
                if (CommandSort.getPermChannels(guild).contains(c.getCategory())) continue;
                ICategory unsorted = guild.getCategoryByID(358043583355289600L);
                c.changeCategory(unsorted);

                try {
                    RequestBuffer.request(() -> {
                        c.overrideRolePermissions(guild.getEveryoneRole(), EnumSet.allOf(Permissions.class), EnumSet.of(Permissions.READ_MESSAGES));
                    });
                    mentions.append(c.mention() + " ");
                } catch (MissingPermissionsException | DiscordException e) {
                    Util.reportHome(e);
                }
            }

            String text = "Opened " + channels.size() + " channel(s).\n" + mentions.toString();
            Util.simpleEmbed(message.getChannel(), text);
            Util.sendLog(message, text);
        } else {
            Util.syntaxError(this, message);
        }
    }
}
