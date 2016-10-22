package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Show;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandShowID implements Command {
    @Override
    public String getName() {
        return "showid";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.DEV_ROLE_ID))) {
            if (args.length >= 1) {
                String showName = Arrays.stream(args).collect(Collectors.joining(" "));
                if (showName.equalsIgnoreCase("here")) {
                    String[] showNameArray = message.getChannel().getName().split("-");
                    showName = Arrays.stream(showNameArray).collect(Collectors.joining(" "));
                }
                Show showData = bot.getTraktManager().showSearch(showName);
                if (showData != null) {
                    Util.sendMessage(message.getChannel(), "Found ID for " + showData.title + ": ``" + showData.ids.imdb + "``");
                } else {
                    Util.sendMessage(message.getChannel(), "Show ID not found");
                }
            } else {
                Util.sendMessage(message.getChannel(), "Usage: !showid [here|showname]");
            }
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }
}