package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandChannelAdd extends Command {

    private TVBot bot;

    public CommandChannelAdd() {
        this.bot = TVBot.getInstance();
        this.name = "addchannel";
        this.aliases = new String[]{"newchannel, createchannel"};
        this.arguments = "addchannel [channelname]";
        this.help = "Creates channels with the provided names. Tip: use \"|\" between multiple names to create multiple channels";
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {

        List<String> channelNames = Arrays.stream(commandEvent.getArgs().split("\\|"))
                .map(arg -> StringUtils.trim(arg).replaceAll("\\s+", "-"))
                .collect(Collectors.toList());

        if (channelNames.size() >= 1 && !StringUtils.isWhitespace(channelNames.get(0))) {
            Message response = Util.simpleEmbedSync(commandEvent.getTextChannel(), "Attempting to create " + channelNames.size() + " channels ...");
            String mentions = createChannels(commandEvent.getGuild(), channelNames);

            Util.deleteMessage(response);

            String text = "Created channel(s):\n" + mentions;
            Util.simpleEmbed(commandEvent.getTextChannel(), text);
            Util.sendLog(commandEvent.getMessage(), text);

            Util.deleteMessage(commandEvent.getMessage());
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private String createChannels(Guild guild, List<String> names) {

        net.dv8tion.jda.core.entities.Category unsorted = guild.getCategoryById(TVBot.UNSORTED_CAT_ID);

        StringBuilder mentions = new StringBuilder();
        for (String s : names) {
            try {
                TextChannel newChannel = (TextChannel) unsorted.createTextChannel(s).complete();
                mentions.append(newChannel.getAsMention()).append("\n");
            } catch (Exception e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }

}
