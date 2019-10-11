package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommandChannelOpen extends Command {

    private TVBot bot;

    public CommandChannelOpen() {
        this.bot = TVBot.getInstance();
        this.name = "openchannel";
        this.aliases = new String[]{"open"};
        this.arguments = "openchannel #channel";
        this.help = "Moves desired channels from the closed category and opens them up to the world.\nUse !openchannel here to open the current channel.";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        List<TextChannel> channels = commandEvent.getMessage().getMentionedChannels();
        if (channels.size() == 0 && args.length >= 1 && args[0].equalsIgnoreCase("here")) {
            channels.add(commandEvent.getTextChannel());
        }

        if(channels.size() >= 1) {
            openChannels(commandEvent.getGuild(), channels);

            String text = "Opened channels:\n" + StringUtils.join(channels.stream().map(IMentionable::getAsMention).toArray(), " ");
            Util.simpleEmbed(commandEvent.getTextChannel(), text);
            Util.sendLog(commandEvent.getMessage(), text);
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private void openChannels(Guild guild, List<TextChannel> channels) {
        var permChannels = Util.getPermChannels(guild);
        for (TextChannel c : channels) {
            if (permChannels.contains(c)) continue;
            var unsortedCategory = guild.getCategoryById(TVBot.UNSORTED_CAT_ID);
            c.getManager().setParent(unsortedCategory).queue();
        }
    }
}
