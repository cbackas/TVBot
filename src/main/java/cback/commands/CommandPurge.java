package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CommandPurge extends Command {

    private TVBot bot;

    public CommandPurge() {
        this.bot = TVBot.getInstance();
        this.name = "purge";
        this.aliases = new String[]{"prune"};
        this.arguments = "purge <#> @user";
        this.help = "For mass deleting messages. It works sometimes I think?";
        this.requiredRole = TVRoles.STAFF.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        var userRoles = commandEvent.getMember().getRoles();
        if (args.length >= 1) {

            String numberArg = args[0];
            int maxDeletions = 0;

            if (StringUtils.isNumeric(numberArg)) maxDeletions = Math.min(100, Integer.parseInt(numberArg));
            if (maxDeletions <= 0) {
                Util.deleteMessage(commandEvent.getMessage());
                Util.simpleEmbed(commandEvent.getTextChannel(), "Invalid number \"" + numberArg + "\".");
                return;
            }

            User userToDelete = null;

            if (args.length >= 2) { //user specified
                userToDelete = Util.getUserFromMentionArg(args[1]);
                if (userToDelete == null) {
                    Util.deleteMessage(commandEvent.getMessage());
                    Util.simpleEmbed(commandEvent.getTextChannel(), "Invalid user \"" + args[1] + "\".");
                    return;
                }
            } else { //no user specified
                if (!userRoles.contains(commandEvent.getGuild().getRoleById(TVRoles.ADMIN.id))) {
                    //Must be admin to purge all without entering user
                    Util.deleteMessage(commandEvent.getMessage());
                    Util.simpleEmbed(commandEvent.getTextChannel(), "You must specify a user.");
                    return;
                }
            }

            if (userToDelete != null) { //this is a prune
                //get last x messages
                var history = commandEvent.getChannel()
                        .getHistoryBefore(commandEvent.getMessage(), maxDeletions)
                        .complete();
                var finalUserToDelete = userToDelete;
                //filter to only the specified user
                var toDelete = history.getRetrievedHistory().stream()
                        .filter(message -> message.getAuthor().getIdLong() == finalUserToDelete.getIdLong())
                        .collect(Collectors.toList());
                //delete them
                commandEvent.getChannel().purgeMessages(toDelete);

                Util.sendLog(commandEvent.getMessage(), userToDelete.getName() + "'s messages have been pruned in " + commandEvent.getChannel().getName() + ".");

            } else { //this is a purge

                //get last x messages
                var history = commandEvent.getChannel()
                        .getHistoryBefore(commandEvent.getMessage(), maxDeletions)
                        .complete();

                //delete them
                commandEvent.getChannel().purgeMessages(history.getRetrievedHistory());

                Util.sendLog(commandEvent.getMessage(), numberArg + " messages have been purged in " + commandEvent.getChannel().getName() + ".");

            }

        } else {
            Util.syntaxError(this, commandEvent.getMessage());
            Util.deleteMessage(commandEvent.getMessage());
            return;
        }

        Util.deleteBufferedMessage(commandEvent.getMessage());
    }
}