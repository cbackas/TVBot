package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
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

    @Override
    public String getSyntax() {
        return "addchannel [channelname]";
    }

    @Override
    public String getDescription() {
        return "Creates channels with the provided names. Tip: use \"|\" between multiple names to create multiple channels";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        String channelName = Arrays.stream(args).collect(Collectors.joining("-"));
        String channelNames[] = channelName.split("-\\|-");

        for (String c : channelNames) {
            RequestBuffer.request(() -> {
                try {
                    guild.createChannel(c);

                    Util.sendLog(message, "Added " + c + " channel.");
                } catch (DiscordException | MissingPermissionsException e) {
                    Util.reportHome(message, e);

                    Util.simpleEmbed(message.getChannel(), c + " channel creation failed.");
                }
            });
        }

        Util.deleteMessage(message);
    }

}
