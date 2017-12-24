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
        if (channelNames.length >= 1) {
            IMessage response = Util.simpleEmbed(message.getChannel(), "Attempting to create " + channelNames.length + " channels ...");

            String mentions = createChannels(guild, channelNames);

            Util.deleteMessage(response);

            String text = "Created " + getCounter() + " channel(s).\n" + mentions;
            Util.simpleEmbed(message.getChannel(), text);
            Util.sendLog(message, text);

            Util.deleteMessage(message);
        } else {
            Util.syntaxError(this, message);
        }
    }

    private String createChannels(IGuild guild, String[] names) {
        StringBuilder mentions = new StringBuilder();
        ICategory unsorted = guild.getCategoryByID(358043583355289600L);
        resetCounter();
        for (String s : names) {
            try {
                RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
                    IChannel c = unsorted.createChannel(s);
                    mentions.append("#" + c.getName() + " ");
                    return true;
                });
                future.get();
                incCounter();
            } catch (MissingPermissionsException | DiscordException e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }

    public int getCounter() {
        return counter;
    }

    public void resetCounter() {
        this.counter = 0;
    }

    public void incCounter() {
        this.counter++;
    }

    int counter = 0;

}
