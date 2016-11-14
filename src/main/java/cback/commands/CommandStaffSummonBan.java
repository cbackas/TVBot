package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

public class CommandStaffSummonBan implements Command {
    @Override
    public String getName() {
        return "staffban";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getRolesForGuild(guild).contains(guild.getRoleByID(TVBot.MOD_ROLE_ID))) {
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                    IUser user = Util.getUserFromMentionArg(args[1]);

                    //Adds someone to the summon ban list
                    if (args[0].equalsIgnoreCase("add")) {
                        if (user != null) {
                            List<String> users = bot.getConfigManager().getConfigArray("cantsummon");
                            users.add(user.getID());

                            bot.getConfigManager().setConfigValue("cantsummon", users);

                            Util.sendMessage(message.getChannel(), "Removed " + user.mention() + "'s ability to summon staff.");

                            Util.botLog(message);
                            Util.deleteMessage(message);
                        }
                    }

                    //Removes someone from the summon ban list
                    if (args[0].equalsIgnoreCase("remove")) {
                        if (user != null) {
                            List<String> users = bot.getConfigManager().getConfigArray("cantsummon");
                            users.remove(user.getID());

                            bot.getConfigManager().setConfigValue("cantsummon", users);

                            Util.sendMessage(message.getChannel(), "Restored " + user.mention() + "'s ability to summon staff.");

                            Util.botLog(message);
                            Util.deleteMessage(message);
                        }
                    }

                } else {
                    Util.sendMessage(message.getChannel(), "**Usage**: ``!staffban [add|remove] @user``");
                }

            }
        }
    }
}
