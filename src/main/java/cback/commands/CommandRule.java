package cback.commands;

import cback.Rules;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;


public class CommandRule extends Command {

    private TVBot bot;

    public CommandRule() {
        this.bot = TVBot.getInstance();
        this.name = "rule";
        this.arguments = "rule #";
        this.help = "Returns the rule requested";
        this.requiredRole = TVRoles.STAFF.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        if(args.length == 1) {
            String ruleNumber = args[0];
            Rules rule = Rules.getRule(ruleNumber);

            if(rule != null) {
                EmbedBuilder ruleEmbed = new EmbedBuilder();

                ruleEmbed
                        .setAuthor(rule.title, null)
                        .setDescription(rule.fullRule);
                Util.sendEmbed(commandEvent.getChannel(), ruleEmbed.setColor(Util.getBotColor()).build());
            } else {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Rule not found");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }
}