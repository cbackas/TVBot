package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.List;

public class CommandCensor extends Command {

    private TVBot bot;

    public CommandCensor() {
        this.bot = TVBot.getInstance();
        this.name = "censor";
        this.arguments = "censor add|remove|list [word]";
        this.help = "Adds or removes a word to be censored from the server";
        this.requiredRole = TVRoles.ADMIN.name;
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        EmbedBuilder bld = new EmbedBuilder();
        List<String> bannedWords = bot.getConfigManager().getConfigArray("bannedwords");

        if(commandEvent.getArgs().length() >= 2) {
            if(commandEvent.getArgs().equalsIgnoreCase("add")) {
                String word = commandEvent.getMessage().getContentRaw().split(" ", 3)[2];
                if(bannedWords.contains(word)) {
                    bld.setDescription(word + " is already a banned word!");
                } else {
                    bannedWords.add(word);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.setDescription(word + " has been added to the list of banned words.");
                }
            } else if(commandEvent.getArgs().equalsIgnoreCase("remove")) {
                String word = commandEvent.getMessage().getContentRaw().split(" ", 3)[2];
                if(bannedWords.contains(word)) {
                    bannedWords.remove(word);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.setDescription(word + " has been removed from the list of banned words.");
                } else {
                    bld.setDescription(word + " is not a censored word... Removed failed.");
                }
            } else {
                bld.addField(getArguments(), getHelp(), false);
            }
        } else if(commandEvent.getArgs().length() == 1 && commandEvent.getArgs().equals("list")) {
            String sexyList = "";

            for(String s : bannedWords) {
                sexyList = sexyList + s + "\n";
            }

            bld.setDescription(sexyList);
        } else {
            bld.addField(getArguments(), getHelp(), false);
        }

        Util.sendEmbed(commandEvent.getChannel(), bld.setColor(Util.getBotColor()).build());
    }
}
