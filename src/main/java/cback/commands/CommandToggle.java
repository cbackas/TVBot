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
        if (args.length == 1) {
            List<String> toggles = bot.getToggleMangager().getToggleList();
            if (args[0].equalsIgnoreCase("list")) {
                ToggleManager tm = bot.getToggleMangager();
                String toggleList = buildToggleList(tm, toggles);
                Util.simpleEmbed(message.getChannel(), "**Toggles**: \n" + toggleList);
            } else if (toggles.contains(args[0])) {
                toggles.stream().filter(toggle -> toggle.equalsIgnoreCase(args[0])).forEach(toggle ->  {
                    boolean state = bot.toggleSetting(toggle);
                    String stateText;
                    if (state) {
                        stateText = "true";
                    } else {
                        stateText = "false";
                    }
                    Util.simpleEmbed(message.getChannel(), "**" + toggle + "** set to state ``" + stateText + "``");
                });
            }
        } else {
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
