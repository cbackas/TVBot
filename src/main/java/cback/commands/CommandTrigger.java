package cback.commands;

import cback.TVBot;
import cback.Util;
import sun.misc.MessageUtils;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.impl.obj.Webhook;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IWebhook;
import sx.blah.discord.util.EmbedBuilder;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTrigger implements Command {
    @Override
    public String getName() {
        return "trigger";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getID().equals("73416411443113984")) {
            String text = Arrays.stream(args).collect(Collectors.joining(" "));

            Util.deleteMessage(message);
        }
    }

}
