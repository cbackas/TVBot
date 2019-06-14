package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandChannelAdd extends Command {

    private TVBot bot;

    public CommandChannelAdd(TVBot bot) {
        this.bot = bot;
        this.name = "addchannel";
        this.aliases = new String[]{"newchannel, createchannel"};
        this.arguments = "addchannel [channelname]";
        this.help = "Creates channels with the provided names. Tip: use \"|\" between multiple names to create multiple channels";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String channelName = Stream.of(commandEvent.getArgs()).collect(Collectors.joining("-"));
        String channelNames[] = channelName.split("-\\|");

        if(channelNames.length >= 1) {
            Message response = Util.simpleEmbed(commandEvent.getChannel(), "Attempting to create" + channelNames.length + " channels ...");
            String mentions = createChannels(commandEvent.getGuild(), channelNames);

            Util.deleteMessage(response);

            String text = "Created " + getCounter() + " channel(s).\n" + mentions;
            Util.simpleEmbed(commandEvent.getChannel(), text);
            Util.sendLog(commandEvent.getMessage(), text);

            Util.deleteMessage(commandEvent.getMessage());
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private String createChannels(Guild guild, String[] names) {
        StringBuilder mentions = new StringBuilder();
        net.dv8tion.jda.core.entities.Category unsorted = guild.getCategoryById(TVBot.UNSORTED_CAT_ID);
        resetCounter();
        for (String s : names) {
            try {
                    ChannelAction c = unsorted.createTextChannel(s);
                    mentions.append("#").append(unsorted.getName()).append(" ");
                incCounter();
            } catch (Exception e) {
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
