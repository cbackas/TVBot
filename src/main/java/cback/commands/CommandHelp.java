package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHelp extends Command {

    private TVBot bot;

    public CommandHelp() {
        this.bot = TVBot.getInstance();
        this.name = "help";
        this.aliases = new String[]{"commands"};
        this.arguments = "help <command>";
        this.help = "Returns a list of commands (you're looking at it right now)";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);
        if (args.length == 1) {
            boolean tripped = false;
            for (Command c : TVBot.registeredCommands) {
                if (c.getName().equalsIgnoreCase(args[0]) || (c.getAliases() != null && Arrays.toString(c.getAliases()).contains(args[0].toLowerCase()))) {
                    Util.syntaxError(c, commandEvent.getMessage());
                    tripped = true;
                    break;
                }
            }

            if (!tripped) {
                Util.simpleEmbed(commandEvent.getTextChannel(), "Sorry, I couldn't find a command named " + args[0]);
            }
        } else {
            EmbedBuilder embed = Util.getEmbed();
            embed.setTitle("Commands:");

            List<Long> roles = commandEvent.getMessage().getAuthor().getJDA().getRoles().stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
            StringBuilder bld = new StringBuilder();
            for (Command c : TVBot.registeredCommands) {
                if (c.getHelp() != null) {
                    String aliases = "Aliases: ";
                    if (c.getAliases() != null) {
                        int commas = c.getAliases().length - 1;
                        for (String a : c.getAliases()) {
                            if (commas > 0) {
                                aliases += a + ", ";
                                commas--;
                            } else {
                                aliases += a;
                            }
                        }
                    }

                    if (c.getUserPermissions() == null || !Collections.disjoint(roles, Arrays.asList(c.getUserPermissions()))) {
                        if (aliases.equals("Aliases: ")) {
                            bld.append("- " + c.getName() + "\n");
                        } else {
                            bld.append("- " + c.getName() + "\n    " + aliases + "\n");
                        }
                    }
                }
            }

            embed.setDescription(bld.toString());
            embed.setFooter("Use " + TVBot.getPrefix() + "help <commandName> to see more info about a command.", null);
            Util.sendEmbed((MessageChannel) commandEvent.getMessage().getAuthor().openPrivateChannel(), embed.setColor(Util.getBotColor()).build());
        }
    }
}