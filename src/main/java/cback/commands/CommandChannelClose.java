package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommandChannelClose extends Command {

    private TVBot bot;

    public CommandChannelClose() {
        this.bot = TVBot.getInstance();
        this.name = "closechannel";
        this.aliases = new String[]{"close"};
        this.arguments = "closechannel #channel";
        this.help = "Closes a TV show channel and makes it all secret.\nUse !closechannel here to close the current channel.";
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
            closeChannels(commandEvent.getGuild(), channels);

            String text = "Closed channels:\n" + StringUtils.join(channels.stream().map(IMentionable::getAsMention).toArray(), " ");
            Util.simpleEmbed(commandEvent.getTextChannel(), text);
            Util.sendLog(commandEvent.getMessage(), text);
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private void closeChannels(Guild guild, List<TextChannel> channels) {
        var permChannels = Util.getPermChannels(guild);
        for (TextChannel c : channels) {
            if (permChannels.contains(c)) continue;
            var closedCategory = guild.getCategoryById(TVBot.CLOSED_CAT_ID);
            c.getManager().setParent(closedCategory).queue();
        }
    }
}