package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.Role;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommandPurge extends Command {

    private TVBot bot;

    public CommandPurge(TVBot bot) {
        this.bot = bot;
        this.name = "purge";
        this.aliases = new String[]{"prune"};
        this.arguments = "prune <#> @user";
        this.help = "For mass deleting messages. It works sometimes I think?";
        this.requiredRole = TVRoles.STAFF.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        List<Role> userRoles = commandEvent.getAuthor().getJDA().getRoles();
        if (args.length >= 1) {

            String numberArg = args[0];

            int maxDeletions = 0;
            Member userToDelete;

            if (StringUtils.isNumeric(numberArg)) {
                try {
                    maxDeletions = Integer.parseInt(numberArg);
                    if (maxDeletions <= 0) {
                        Util.deleteMessage(commandEvent.getMessage());
                        Util.simpleEmbed(commandEvent.getChannel(), "Invalid number \"" + numberArg + "\".");
                        return;
                    }
                } catch (NumberFormatException e) {
                }
            }

            if (args.length >= 2) { //user specified
                userToDelete = Util.getUserFromMentionArg(args[1]);
                if (userToDelete == null) {
                    Util.deleteMessage(commandEvent.getMessage());
                    Util.simpleEmbed(commandEvent.getChannel(), "Invalid user \"" + args[1] + "\".");
                    return;
                }
            } else {
                userToDelete = null;
                if (!userRoles.contains(commandEvent.getGuild().getRoleById(TVRoles.ADMIN.id))) {
                    //Must be admin to purge all without entering user
                    Util.deleteMessage(commandEvent.getMessage());
                    Util.simpleEmbed(commandEvent.getChannel(), "You must specify a user.");
                    return;
                }
            }

            //sort messages by date
            MessageHistory messageHistory = commandEvent.getChannel().getHistory();
            messageHistory.getRetrievedHistory();

            if (userToDelete != null) { //this is a prune

                commandEvent.getChannel().getHistoryBefore(commandEvent.getMessage(), maxDeletions).queue((history) -> commandEvent.getChannel().purgeMessages(history.getRetrievedHistory()));
                Util.sendLog(commandEvent.getMessage(), userToDelete.getEffectiveName() + "'s messages have been pruned in " + commandEvent.getChannel().getName() + ".");

            } else { //this is a purge

                commandEvent.getChannel().getHistoryBefore(commandEvent.getMessage(), numberArg.length()).queue((history) -> commandEvent.getChannel().purgeMessages(history.getRetrievedHistory()));
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