package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHelp implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("commands");
    }

    @Override
    public String getSyntax() {
        return "help <command>";
    }

    @Override
    public String getDescription() {
        return "Returns a list of commands (you're looking at it right now)";
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        if (args.length == 1) {
            boolean tripped = false;
            for (Command c : TVBot.registeredCommands) {
                if (c.getName().equalsIgnoreCase(args[0]) || (c.getAliases() != null && c.getAliases().contains(args[0].toLowerCase()))) {
                    Util.syntaxError(c, message);
                    tripped = true;
                    break;
                }
            }

            if (!tripped) {
                Util.simpleEmbed(message.getChannel(), "Sorry, I couldn't find a command named " + args[0]);
            }
        } else {
            EmbedBuilder embed = Util.getEmbed();
            embed.withTitle("Commands:");

            List<Long> roles = message.getAuthor().getRolesForGuild(guild).stream().map(role -> role.getLongID()).collect(Collectors.toList());
            StringBuilder bld = new StringBuilder();
            for (Command c : TVBot.registeredCommands) {

                if (c.getDescription() != null) {

                    String aliases = "Aliases: ";
                    if (c.getAliases() != null) {
                        int commas = c.getAliases().size() - 1;
                        for (String a : c.getAliases()) {
                            if (commas > 0) {
                                aliases += a + ", ";
                                commas--;
                            } else {
                                aliases += a;
                            }
                        }
                    }

                    if (c.getPermissions() == null || !Collections.disjoint(roles, c.getPermissions())) {
                        if (aliases.equals("Aliases: ")) {
                            bld.append("- " + c.getName() + "\n");
                        } else {
                            bld.append("- " + c.getName() + "\n    " + aliases + "\n");
                        }
                    }

                }

            }

            embed.withDesc(bld.toString());

            embed.withFooterText("Use " + TVBot.getPrefix() + "help <commandName> to see more info about a command.");

            Util.sendEmbed(message.getAuthor().getOrCreatePMChannel(), embed.withColor(Util.getBotColor()).build());
        }

        Util.deleteMessage(message);

    }

}