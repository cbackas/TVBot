package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ISnowflake;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHelp extends Command {

    private TVBot bot;

    public CommandHelp() {
        this.bot = TVBot.getInstance();
        this.name = "help";
        this.aliases = new String[]{"commands"};
        this.arguments = "help <command>";
        this.help = "Returns a list of commands or the syntax for a given command.";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());
        if (args.length >= 1) {

            var result = bot.getCommandClient().getCommands().stream()
                    .filter(command -> args[0].equalsIgnoreCase(command.getName()))
                    .findAny();

            if (result.isPresent()) {
                Util.syntaxError(result.get(), commandEvent.getMessage());
            } else {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Sorry, I couldn't find a command named " + args[0]);
            }
        } else {
            EmbedBuilder embed = Util.getEmbed();
            embed.setTitle("Commands:");

            List<Long> roles = commandEvent.getMessage().getAuthor().getJDA().getRoles().stream().map(ISnowflake::getIdLong).collect(Collectors.toList());

            StringBuilder bld = new StringBuilder();
            List<Command> sortedCommands = new ArrayList<>(bot.getCommandClient().getCommands());
            sortedCommands.sort(Comparator.comparing(Command::getName));
            for (Command c : bot.getCommandClient().getCommands()) {
                if (c.getHelp() != null && !c.isOwnerCommand()) {

                    boolean hasPerms =
                            c.getRequiredRole() == null || bot.getHomeGuild().getMember(commandEvent.getAuthor()).getRoles().stream().anyMatch(role -> c.getRequiredRole().equalsIgnoreCase(role.getName()));

                    if (hasPerms) {
                        if (c.getAliases().length == 0) {
                            bld.append("- ").append(c.getName()).append("\n");
                        } else {
                            String aliases = " (" + StringUtils.join(c.getAliases(), ",") + ")";
                            bld.append("- ").append(c.getName()).append(aliases).append("\n");
                        }
                    }
                }
            }

            embed.setDescription(bld.toString());
            embed.setFooter("Use " + TVBot.COMMAND_PREFIX + "help <commandName> to see more info about a command.", null);
            Util.sendEmbed(commandEvent.getMessage().getAuthor().openPrivateChannel().complete(), embed.setColor(Util.getBotColor()).build());
        }
    }
}