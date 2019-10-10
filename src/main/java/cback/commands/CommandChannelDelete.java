package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;

public class CommandChannelDelete extends Command {

    private TVBot bot;

    public CommandChannelDelete() {
        this.bot = TVBot.getInstance();
        this.name = "deletechannel";
        this.aliases = new String[]{"removechannel"};
        this.arguments = "deletechannel #channel";
        this.help = "Deletes any and all mentioned channels you provide. Use 'here' to delete the current channel";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        List<TextChannel> toDelete = new ArrayList<>(commandEvent.getMessage().getMentionedChannels());
        TextChannel here = commandEvent.getTextChannel();
        if (toDelete.size() == 0 && commandEvent.getArgs().equalsIgnoreCase("here")) {
            toDelete.add(here);
        }

        if (toDelete.size() >= 1) {
            String mentions = deleteChannels(toDelete);

            String text = "Deleted " + toDelete.size() + " channel(s).\n" + mentions;
            if (!toDelete.contains(here)) {
                Util.simpleEmbed(commandEvent.getTextChannel(), text);
            }

            Util.sendLog(commandEvent.getMessage(), text);
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }

    private String deleteChannels(List<TextChannel> channels) {
        StringBuilder mentions = new StringBuilder();
        for (TextChannel c : channels) {
            try {
                c.delete().queue();
                mentions.append("#" + c.getName() + " ");
            } catch (Exception e) {
                Util.reportHome(e);
            }
        }
        return mentions.toString();
    }
}
