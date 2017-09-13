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
        return Arrays.asList(TVRoles.ADMIN.id, TVRoles.MOD.id);
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        EmbedBuilder bld = new EmbedBuilder();
        List<String> bannedWords = bot.getConfigManager().getConfigArray("bannedWords");

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                if (bannedWords.contains(args[1])) {
                    bld.withDesc(args[1] + " is already a banned word!");
                } else {
                    bannedWords.add(args[1]);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.withDesc(args[1] + " has been added to the list of banned words.");
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (bannedWords.contains(args[1])) {
                    bannedWords.remove(args[1]);
                    bot.getConfigManager().setConfigValue("bannedWords", bannedWords);

                    bld.withDesc(args[1] + " has been removed from the list of banned words.");
                } else {
                    bld.withDesc(args[1] + " is not a censored word... Remove failed.");
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
