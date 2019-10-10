package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class CommandChannelClose extends Command {

    private TVBot bot;

    public CommandChannelClose() {
        this.bot = TVBot.getInstance();
        this.name = "closechannel";
        this.aliases = new String[]{"close"};
        this.arguments = "closechannel #channel";
        this.help = "Closes a TV show channel and makes it all secret. If you don't specify a channel, it will just close";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }
    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        List<TextChannel> channels = commandEvent.getMessage().getMentionedChannels();
        if(channels.size() == 0 && args[0].equalsIgnoreCase("here")) {
            channels.add(commandEvent.getTextChannel());
        }

        if(channels.size() >= 1) {
            String mentions = closeChannels(commandEvent.getGuild(), channels);

            String text = "Closed " + channels.size() + " channel(s).\n" + mentions;
            Util.simpleEmbed(commandEvent.getTextChannel(), text);
            Util.sendLog(commandEvent.getMessage(), text);
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private String closeChannels(Guild guild, List<TextChannel> channels) {
        StringBuilder mentions = new StringBuilder();
        for (Channel c : channels) {
            if (Util.getPermChannels(guild).contains(c)) continue;
            net.dv8tion.jda.core.entities.Category closed = guild.getCategoryById(355904962200469504L);
            c.getManager().setParent(closed).queue();

            try {
                c.createPermissionOverride(guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).queue();
                mentions.append("#").append(c.getName()).append(" ");
            } catch (Exception e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }
}