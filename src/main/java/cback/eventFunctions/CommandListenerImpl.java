package cback.eventFunctions;

import cback.Channels;
import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.nibor.autolink.LinkExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommandListenerImpl implements CommandListener {

    private TVBot bot = TVBot.getInstance();

    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?s)^" + TVBot.COMMAND_PREFIX + "([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);
    private static LinkExtractor linkExtractor = LinkExtractor.builder().build();

    @Override
    public void onCommand(CommandEvent event, Command command) {
        Util.getLogger().info(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + " issued command in " + event.getChannel().getName() + " - " + event.getMessage().getContentRaw());
    }

    @Override
    public void onNonCommandMessage(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return; //ignore bot messages
        boolean isPrivate = event.getMessage().isFromType(ChannelType.PRIVATE);

        var message = event.getMessage();
        String text = event.getMessage().getContentRaw();

        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.matches()) {
            //check for custom command
            String baseCommand = matcher.group(1).toLowerCase();
            if (bot.getCustomCommandManager().getCommandValue(baseCommand) != null) {
                //send custom response
                String response = bot.getCustomCommandManager().getCommandValue(baseCommand);
                Util.sendMessage(message.getTextChannel(), "``" + message.getAuthor().getName() + "``\n" + response);
                Util.deleteMessage(message);
            }

        } else if (isPrivate) {
            //Forwards the random stuff people PM to the bot - to me
            MessageEmbed embed = Util.buildBotPMEmbed(message, 1);
            Util.sendEmbed(Channels.BOTPM_CH_ID.getChannel(), embed);
        } else {
            //below here are just regular chat messages

            //Increment message count if message was not a command
            bot.getDatabaseManager().getXP().addXP(message.getAuthor().getId(), 1);

            //Messages containing my name go to botpms now too cuz im watching//
            if (message.getContentRaw().toLowerCase().contains("cback")) {
                MessageEmbed embed = Util.buildBotPMEmbed(message, 2);
                Util.sendEmbed(Channels.BOTPM_CH_ID.getChannel(), embed);
            }
        }
    }
}