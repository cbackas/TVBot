package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandChannelAdd implements Command {
    @Override
    public String getName() {
        return "addchannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("newchannel", "createchannel");
    }

    public static List<String> permitted = Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id, TVRoles.HEADMOD.id);

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<String> userRoles = message.getAuthor().getRolesForGuild(guild).stream().map(role ->role.getID()).collect(Collectors.toList());
        if (!Collections.disjoint(userRoles, permitted)) {

            String channelName = Arrays.stream(args).collect(Collectors.joining("-"));
            String channelNames[] = channelName.split("-\\|-");

            for (String c : channelNames) {
                RequestBuffer.request(() -> {
                    try {
                        guild.createChannel(c);

                        Util.sendLog(message, "Added " + c + " channel.");
                    } catch (DiscordException | MissingPermissionsException e) {
                        e.printStackTrace();

                        Util.sendMessage(message.getChannel(), "**" + c + "** channel creation failed.");
                    }
                });
            }

            Util.botLog(message);
            Util.deleteMessage(message);
        }
    }

}
