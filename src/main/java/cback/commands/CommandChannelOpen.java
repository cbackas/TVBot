package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class CommandChannelOpen extends Command {

    private TVBot bot;

    public CommandChannelOpen() {
        this.bot = TVBot.getInstance();
        this.name = "openchannel";
        this.aliases = new String[]{"open"};
        this.arguments = "openchannel #channel";
        this.help = "Moves desired channels from the closed category and opens them up to the world.";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        List<TextChannel> channels = commandEvent.getMessage().getMentionedChannels();
        if(channels.size() == 0 && commandEvent.getArgs().equalsIgnoreCase("here")) {
            channels.add(commandEvent.getTextChannel());
        }

        if(channels.size() >= 1) {
            String mentions = openChannels(commandEvent.getGuild(), channels);

            String text = "Opened " + channels.size() + " channel(s).\n" + mentions;
            Util.simpleEmbed(commandEvent.getTextChannel(), text);
            Util.sendLog(commandEvent.getMessage(), text);
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private String openChannels(Guild guild, List<TextChannel> channels) {
        StringBuilder mentions = new StringBuilder();
        for (TextChannel c : channels) {
            if (Util.getPermChannels(guild).contains(c)) continue;
            net.dv8tion.jda.core.entities.Category unsorted = guild.getCategoryById(358043583355289600L);
            c.getManager().setParent(unsorted).queue();

            try {
                c.putPermissionOverride(guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).queue();
                mentions.append("#" + c.getName() + " ");
            } catch (Exception e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }
}
