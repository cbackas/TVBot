package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.Arrays;
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
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {

            String channelName = Arrays.stream(args).collect(Collectors.joining("-"));
            String channelNames[] = channelName.split("-\\|-");

            for (String c : channelNames) {
                RequestBuffer.request(() -> {
                    try {
                        guild.createChannel(c);

                        Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```Added " + channelName + " channel.\n- " + message.getAuthor().getDisplayName(guild) + "```");
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
