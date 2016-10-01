package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.*;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandMute implements Command {
    @Override
    public String getName() {
        return "mute";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (args.length == 1) {
            String user = args[0];
            Pattern pattern = Pattern.compile("^<@(.+)>");
            Matcher matcher = pattern.matcher(user);
            if (matcher.find()) {
                String u = matcher.group(1);
                IUser userInput = guild.getUserByID(u);
                List<IRole> roles = userInput.getRolesForGuild(guild);
                try {
                    DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                    if (message.getAuthor().getID().equals(u)) {
                        Util.sendMessage(message.getChannel(), "You probably shouldn't mute yourself");
                    } else if (roles.contains(guild.getRoleByID("231269949635559424"))) {
                            try {
                                userInput.removeRole(guild.getRoleByID("231269949635559424"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been unmuted");
                            } catch (Exception e) {
                                Util.sendMessage(message.getChannel(), "That user can't be muted");
                            }
                        } else {
                            try {
                                userInput.addRole(guild.getRoleByID("231269949635559424"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + " has been muted");
                            } catch (Exception e) {
                                Util.sendMessage(message.getChannel(), "That user can't be muted");
                            }
                        }
                } catch(Exception e){
                        e.printStackTrace();
                        Util.sendMessage(message.getChannel(), "You don't have permission to mute members");
                    }
                }
        } else {
            Util.sendMessage(message.getChannel(), "Too many arguments");
        }
    }
}
