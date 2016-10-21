package cback.commands;

import cback.TVBot;
import cback.Util;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.MessageList;

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
        if (userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID)) || userRoles.contains(guild.getRoleByID(TVBot.MOD_ROLE_ID))) {

            //find the first argument that is an integer and use it as the number to purge/prune
            int numberArg = 0;
            for (String arg : args)
                if (StringUtils.isNumeric(arg)) {
                    try {
                        numberArg = Integer.parseInt(arg);
                        break;
                    } catch (NumberFormatException e) {
                    }
                }

            if (numberArg <= 0) {
                Util.deleteMessage(message);
                Util.sendMessage(message.getChannel(), "Invalid arguments. Usage: ``!prune @user <#> OR !purge <#>`` (commands are interchangeable, user and # can be specified in any order)");
                return;
            }


            if (message.getMentions().size() > 0) { //this is a prune - delete only from user

                IUser userToDelete = message.getMentions().get(0);
                List<IMessage> toDelete = message.getChannel().getMessages().stream()
                        .filter(msg -> msg.getAuthor().equals(userToDelete) && !msg.equals(message))
                        .limit(numberArg)
                        .collect(Collectors.toList());

                Util.bulkDelete(message.getChannel(), toDelete);
                Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + userToDelete.getDisplayName(guild) + "'s messages have been pruned in " + message.getChannel().getName() + ".\n- " + message.getAuthor().getDisplayName(guild) + "```");

            } else { //this is a purge - delete all
                if (userRoles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) {

                    List<IMessage> toDelete = message.getChannel().getMessages().stream()
                            .filter(msg -> !msg.equals(message))
                            .limit(numberArg)
                            .collect(Collectors.toList());

                    Util.bulkDelete(message.getChannel(), toDelete);
                    Util.sendMessage(guild.getChannelByID(TVBot.LOG_CHANNEL_ID), "```" + numberArg + " messages have been purged in " + message.getChannel().getName() + ".\n- " + message.getAuthor().getDisplayName(guild) + "```");

                } else {
                    Util.sendMessage(message.getChannel(), "No permission, please specify a user.");
                }
            }
            Util.deleteBufferedMessage(message);
        }
    }

    @Override
    public boolean isLogged() {
        return true;
    }

}
