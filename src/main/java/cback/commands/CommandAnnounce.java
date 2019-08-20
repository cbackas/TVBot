package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandAnnounce extends Command {

    private TVBot bot;

    public CommandAnnounce() {
        this.bot = TVBot.getInstance();
        this.name = "announce";
        this.help = "Sends a message in the announcement channel and the general channel";
        this.arguments = "announce [message]";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        if(arguments.length() >= 1) {
            String announcement = commandEvent.getMessage().getContentRaw().split(" ", 2)[1];
            Util.sendAnnouncement(announcement);
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
