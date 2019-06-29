package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.util.Collections;
import java.util.List;

public class CommandSuggest extends Command {

    private TVBot bot;

    public CommandSuggest(TVBot bot) {
        this.bot = bot;
        this.name = "suggest";
        this.aliases = new String[]{"idea", "suggestion"};
        this.arguments = "suggest [suggestion text]";
        this.help = "Pins your message, making it an official suggestion.";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        List<Long> suggestable = Collections.singletonList(Channels.SUGGEST_CH_ID.getChannel().getIdLong());
        if(suggestable.contains(commandEvent.getChannel().getIdLong())) {
            try {
                commandEvent.getTextChannel().pinMessageById(commandEvent.getMessage().getIdLong()).queue();
            } catch(Exception ex) {
                Util.reportHome(commandEvent.getMessage(), ex);
            }
        }
    }
}