package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class CommandChannelClose extends Command {

    private TVBot bot;

    public CommandChannelClose(TVBot bot) {
        this.bot = bot;
        this.name = "closechannel";
        this.aliases = new String[]{"close"};
        this.arguments = "closechannel #channel";
        this.help = "Closes a TV show channel and makes it all secret. If you don't specify a channel, it will just close";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }
    @Override
    protected void execute(CommandEvent commandEvent) {

    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<IChannel> channels = message.getChannelMentions();
        if (channels.size() == 0 && args[0].equalsIgnoreCase("here")) {
            channels.add(message.getChannel());
        }

        if (channels.size() >= 1) {
            String mentions = closeChannels(guild, channels);

            String text = "Closed " + channels.size() + " channel(s).\n" + mentions;
            Util.simpleEmbed(message.getChannel(), text);
            Util.sendLog(message, text);
        } else {
            Util.syntaxError(this, message);
        }
    }

    private String closeChannels(Guild guild, List<TextChannel> channels) {
        StringBuilder mentions = new StringBuilder();
        for (Channel c : channels) {
            if (CommandSort.getPermChannels(guild).contains(c.getCategory())) continue;
            net.dv8tion.jda.core.entities.Category closed = guild.getCategoryById(355904962200469504L);
            c.changeCategory(closed);

            try {
                RequestBuffer.RequestFuture<Boolean> future = RequestBuffer.request(() -> {
                    c.overrideRolePermissions(guild.getEveryoneRole(), EnumSet.noneOf(Permission.class), EnumSet.of(Permission.MESSAGE_READ));
                    return true;
                });
                future.get();
                mentions.append("#").append(c.getName()).append(" ");
            } catch (Exception e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }
}
