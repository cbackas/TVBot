package cback.eventFunctions;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommandListenerImpl implements CommandListener {

    private static Logger log = LoggerFactory.getLogger(CommandListenerImpl.class);

    @Override
    public void onCommand(CommandEvent event, Command command) {
        log.info(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + " issued command in " + event.getChannel().getName() + " - " + event.getMessage().getContentRaw());
    }

    @Override
    public void onNonCommandMessage(MessageReceivedEvent event) {
        //TODO move some logic here
    }
}
