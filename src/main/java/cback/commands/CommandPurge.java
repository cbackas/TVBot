package cback.commands;

import cback.TVBot;
import cback.Util;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPurge implements Command {

    @Override
    public String getName() {
        return "purge";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("prune");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.DEV_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.MOD_ROLE_ID))) {

            if (args.length >= 1) {

                String numberArg = args[0];

                int maxDeletions = 0;
                IUser userToDelete;

                if (StringUtils.isNumeric(numberArg)) {
                    try {
                        maxDeletions = Integer.parseInt(numberArg);
                        if (maxDeletions <= 0) {
                            Util.deleteMessage(message);
                            Util.sendMessage(message.getChannel(), "Invalid number \"" + numberArg + "\".");
                            return;
                        }
                    } catch (NumberFormatException e) {
                    }
                }

                if (args.length >= 2) { //user specified
                    userToDelete = Util.getUserFromMentionArg(args[1]);
                    if (userToDelete == null) {
                        Util.deleteMessage(message);
                        Util.sendMessage(message.getChannel(), "Invalid user \"" + args[1] + "\".");
                        return;
                    }
                } else {
                    userToDelete = null;
                    if (!userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) && !userRoles.contains(guild.getRoleByID(TVBot.DEV_ROLE_ID))) {
                        //Must be admin to purge all without entering user
                        Util.deleteMessage(message);
                        Util.sendMessage(message.getChannel(), "You must specify a user.");
                        return;
                    }
                }

                if (userToDelete != null) { //this is a prune

                    List<IMessage> toDelete = message.getChannel().getMessages().stream()
                            .filter(msg -> msg.getAuthor().equals(userToDelete) && !msg.equals(message))
                            .limit(maxDeletions)
                            .collect(Collectors.toList());

                    Util.bulkDelete(message.getChannel(), toDelete);
                    Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + userToDelete.getDisplayName(guild) + "'s messages have been pruned in " + message.getChannel().getName() + ".\n- " + message.getAuthor().getDisplayName(guild) + "```");

                } else { //this is a purge

                    List<IMessage> toDelete = message.getChannel().getMessages().stream()
                            .filter(msg -> !msg.equals(message))
                            .limit(maxDeletions)
                            .collect(Collectors.toList());

                    Util.bulkDelete(message.getChannel(), toDelete);
                    Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + numberArg + " messages have been purged in " + message.getChannel().getName() + ".\n- " + message.getAuthor().getDisplayName(guild) + "```");

                }

            } else {
                Util.deleteMessage(message);
                Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!prune <#> @user``");
                return;
            }

            Util.deleteBufferedMessage(message);
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }

}
