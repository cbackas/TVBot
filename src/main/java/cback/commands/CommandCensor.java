package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommandCensor extends Command {

    private TVBot bot;

    public CommandCensor() {
        this.bot = TVBot.getInstance();
        this.name = "censor";
        this.arguments = "censor add|remove|list [word]";
        this.help = "Adds or removes a word to be censored from the server";
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {

        String[] args = Util.splitArgs(commandEvent.getArgs());

        EmbedBuilder bld = new EmbedBuilder();
        List<String> bannedWords = bot.getConfigManager().getConfigArray("bannedwords");

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("add")) {
                String word = commandEvent.getArgs().split("\\s+", 2)[1];
                if(bannedWords.contains(word)) {
                    bld.setDescription(word + " is already a banned word!");
                } else {
                    bannedWords.add(word);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.setDescription(word + " has been added to the list of banned words.");
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                String word = commandEvent.getArgs().split("\\s+", 2)[1];
                if(bannedWords.contains(word)) {
                    bannedWords.remove(word);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.setDescription(word + " has been removed from the list of banned words.");
                } else {
                    bld.setDescription(word + " is not a censored word... Removed failed.");
                }
            } else {
                bld.setDescription(getHelp());
            }
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            bld.setDescription(StringUtils.join(bannedWords, "\n"));
        } else {
            bld.setDescription(getHelp());
        }

        Util.sendEmbed(commandEvent.getChannel(), bld.setColor(Util.getBotColor()).build());
    }
}
