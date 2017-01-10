package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandEmbedMuteAdd implements Command {
    @Override
    public String getName() {
        return "embedmute";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVRoles.STAFF.id))) {
            if (userRoles.contains(guild.getRoleByID(TVRoles.HELPER.id)) || userRoles.contains(guild.getRoleByID(TVRoles.ADMIN.id)) || userRoles.contains(guild.getRoleByID(TVRoles.MOD.id))) {

                Util.botLog(message);

                if (args.length >= 1) {
                    String text = message.getContent();
                    Pattern pattern = Pattern.compile("^!embedmute <@!?(\\d+)> ?");
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        String u = matcher.group(1);
                        IUser userInput = guild.getUserByID(u);
                        if (message.getAuthor().getID().equals(u)) {
                            Util.sendMessage(message.getChannel(), "You probably shouldn't mute yourself");
                        } else {
                            try {
                                userInput.addRole(guild.getRoleByID("239233306325942272"));
                                Util.sendMessage(message.getChannel(), userInput.getDisplayName(guild) + "'s embed/attach-files permission has been suspended.");
                                Util.sendLog(message, userInput.getDisplayName(guild) + "'s embed/attach-files permission has been suspended.", Color.black);
                                Util.deleteMessage(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!embedmute @user``");
                }
            }
        }
    }

}
