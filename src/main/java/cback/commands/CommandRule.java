package cback.commands;

import cback.Rules;
import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.MessageBuilder;

import java.util.EnumSet;


public class CommandRule implements Command {
    @Override
    public String getName() {
        return "rule";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length == 1) {
            String ruleNumber = args[0];
            Rules rule = Rules.getRule(ruleNumber);
            if (rule != null) {
                try {
                    DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                    new MessageBuilder(client).withChannel(message.getChannel()).appendContent("**" + rule.title + "**").appendQuote(rule.specifics).send();
                } catch (Exception e) {
                }
            } else {
                Util.sendMessage(message.getChannel(), "Rule not found");
            }
        } else {
            Util.sendMessage(message.getChannel(), "Too many arguments");
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
