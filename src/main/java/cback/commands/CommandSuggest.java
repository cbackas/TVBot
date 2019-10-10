package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class CommandSuggest extends Command {

    private TVBot bot;

    public CommandSuggest() {
        this.bot = TVBot.getInstance();
        this.name = "suggest";
        this.aliases = new String[]{"idea", "suggestion"};
        this.arguments = "suggest [suggestion text]";
        this.help = "Pins your message, making it an official suggestion.";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        if (commandEvent.getChannel().getIdLong() == Channels.SUGGEST_CH_ID.getId()) {
            try {
                commandEvent.getTextChannel().pinMessageById(commandEvent.getMessage().getIdLong()).queue();
            } catch(Exception ex) {
                Util.reportHome(commandEvent.getMessage(), ex);
            }
        }
    }
}