package cback.commands;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;

import java.util.EnumSet;

public class modCommands {

    @EventSubscriber
    public void modCommands(MessageReceivedEvent event) {
        IMessage message = event.getMessage();
        String text = message.getContent();
    }

}
