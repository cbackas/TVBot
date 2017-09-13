package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import java.util.Arrays;
import java.util.List;

public class CommandLog implements Command {
    @Override
    public String getName() {
        return "addlog";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("log");
    }

    @Override
    public String getSyntax() {
        return "addlog [message]";
    }

    @Override
    public String getDescription() {
        return "Submits a serverlog with some info attached";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.STAFF.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length >= 1) {
            String finalText = message.getFormattedContent().split(" ", 2)[1];
            Util.sendLog(message, finalText);
            Util.simpleEmbed(message.getChannel(), "Log added. " + guild.getChannelByID(TVBot.SERVERLOG_CH_ID).mention());
            Util.deleteMessage(message);
        } else {
            Util.syntaxError(this, message);
        }
    }

}
