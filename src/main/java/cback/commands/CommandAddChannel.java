package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandAddChannel implements Command {
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
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) | message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.DEV_ROLE_ID))) {
            String channelName = Arrays.stream(args).collect(Collectors.joining("-"));
            try {
                guild.createChannel(channelName);
                Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```Added " + channelName + " channel.\n- " + message.getAuthor().getDisplayName(guild) + "```");
            } catch (Exception e) {
                e.printStackTrace();
                Util.sendMessage(message.getChannel(), "Channel creation failed.");
            }
            Util.deleteMessage(message);
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
