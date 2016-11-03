package cback.commands;

import cback.TVBot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class CommandTrigger implements Command {
    @Override
    public String getName() {
        return "trigger";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getID().equals("73416411443113984")) {
            List<String> permChannels = bot.getConfigManager().getConfigArray("permanentchannels");
            permChannels.forEach(id -> {
                IChannel channel = guild.getChannelByID(id);
                System.out.println(id + " " + (channel == null ? "null" : channel.getName()));
            });
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
