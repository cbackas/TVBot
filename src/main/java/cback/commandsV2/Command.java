package cback.commandsV2;

import cback.TVBot;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.List;

@Getter
public abstract class Command {
    protected TVBot bot;
    protected CommandData commandData;
    protected List<CommandPrivilege> commandPrivileges;

    public Command() {
        this.bot = TVBot.getInstance();
    }

    public abstract void execute(SlashCommandEvent var1);
}

