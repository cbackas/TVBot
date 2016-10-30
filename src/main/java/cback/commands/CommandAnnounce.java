package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandAnnounce implements Command {
    @Override
    public String getName() {
        return "announce";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {
            if (args.length >= 1) {
                Util.deleteMessage(message);

                String announcement = Arrays.stream(args).collect(Collectors.joining(" "));
                Util.sendAnnouncement(announcement);
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}
