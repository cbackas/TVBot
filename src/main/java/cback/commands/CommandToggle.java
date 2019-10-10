package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.ToggleManager;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.util.List;

public class CommandToggle extends Command {

    private TVBot bot;

    public CommandToggle() {
        this.bot = TVBot.getInstance();
        this.name = "toggle";
        this.aliases = new String[]{"switch"};
        this.arguments = "toggle [setting]";
        this.requiredRole = TVRoles.ADMIN.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = Util.splitArgs(commandEvent.getArgs());

        if(args.length == 1) {
            List<String> toggles = bot.getToggleManager().getToggleList();
            if(args[0].equalsIgnoreCase("list")) {
                ToggleManager tm = bot.getToggleManager();
                String toggleList = buildToggleList(tm, toggles);
                Util.simpleEmbed(commandEvent.getTextChannel(), "**Toggles**: \n" + toggleList);
            } else if(toggles.contains(args[0])) {
                toggles.stream().filter(toggle -> toggle.equalsIgnoreCase(args[0])).forEach(toggle -> {
                    boolean state = bot.toggleSetting(toggle);
                    String stateText;
                    if(state) {
                        stateText = "true";
                    } else {
                        stateText = "false";
                    }
                    Util.simpleEmbed(commandEvent.getTextChannel(), "**" + toggle + "** set to state ``" + stateText + "``");
                });
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
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