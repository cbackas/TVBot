package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.util.Arrays;
import java.util.List;

public class CommandCensor implements Command {
    @Override
    public String getName() {
        return "censor";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "censor add|remove|list [word]";
    }

    @Override
    public String getDescription() {
        return "adds or removes a word to be censored from the server";
    }

    @Override
    public List<Long> getPermissions() {
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        EmbedBuilder bld = new EmbedBuilder();
        List<String> bannedWords = bot.getConfigManager().getConfigArray("bannedWords");

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("add")) {
                String word = content.split(" ", 3)[2];
                if (bannedWords.contains(word)) {
                    bld.withDesc(word + " is already a banned word!");
                } else {
                    bannedWords.add(word);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.withDesc(word + " has been added to the list of banned words.");
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                String word = content.split(" ", 3)[2];
                if (bannedWords.contains(word)) {
                    bannedWords.remove(word);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.withDesc(word + " has been removed from the list of banned words.");
                } else {
                    bld.withDesc(word + " is not a censored word... Remove failed.");
                }
            } else {
                bld.appendField(getSyntax(), getDescription(), false);
            }
        } else if (args.length == 1 && args[0].equals("list")) {
            String sexyList = "";

            for (String s : bannedWords) {
                sexyList = sexyList + s + "\n";
            }

            bld.withDesc(sexyList);
        } else {
            bld.appendField(getSyntax(), getDescription(), false);
        }

        Util.sendEmbed(message.getChannel(), bld.withColor(Util.getBotColor()).build());
    }
}
