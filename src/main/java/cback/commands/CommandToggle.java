package cback.commands;

import cback.TVBot;
import cback.ToggleManager;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;

public class CommandToggle implements Command {
    @Override
    public String getName() {
        return "toggle";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("switch");
    }

    @Override
    public String getSyntax() {
        return "toggle [setting]";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public List<Long> getPermissions() {
        return null;
    }

    @Override
    public void execute(IMessage message, String content, String[] args, IUser author, IGuild guild, List<Long> roleIDs, boolean isPrivate, IDiscordClient client, TVBot bot) {
        List<String> toggles = bot.getToggleMangager().getToggleList();
        if (args.length == 1 && toggles.contains(args[0])) {
            toggles.stream().filter(toggle -> toggle.equalsIgnoreCase(args[0])).forEach(toggle -> bot.toggleSetting(toggle));
        } else {
            ToggleManager tm = bot.getToggleMangager();
            String toggleList = buildToggleList(tm, toggles);

            Util.simpleEmbed(message.getChannel(), "**Toggles**:\n" + toggleList);
            Util.syntaxError(this, message);
        }
    }

    private String buildToggleList(ToggleManager tm, List<String> toggles) {
        StringBuilder bld = new StringBuilder();
        for (String s : toggles) {
            boolean setting = tm.getToggleValue(s);
            bld.append(s + " - ");
            if (setting) {
                bld.append("true");
            } else {
                bld.append("false");
            }
            bld.append("\n");
        }
        return bld.toString();
    }
}
