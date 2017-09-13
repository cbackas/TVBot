package cback.commands;

import cback.Rules;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.util.Arrays;
import java.util.List;


public class CommandRule implements Command {
    @Override
    public String getName() {
        return "rule";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "rule #";
    }

    @Override
    public String getDescription() {
        return "Returns the rule requested";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.STAFF.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length == 1) {
            String ruleNumber = args[0];
            Rules rule = Rules.getRule(ruleNumber);

            if (rule != null) {
                EmbedBuilder ruleEmbed = new EmbedBuilder();

                ruleEmbed
                        .withAuthorName(rule.title)
                        .withDescription(rule.fullRule);

                Util.sendEmbed(message.getChannel(), ruleEmbed.withColor(Util.getBotColor()).build());
            } else {
                Util.simpleEmbed(message.getChannel(), "Rule not found");
            }
        } else {
            Util.syntaxError(this, message);
        }
    }

}
