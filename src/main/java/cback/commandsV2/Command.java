package cback.commandsV2;

import cback.TVBot;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Getter
public abstract class Command {
    protected TVBot bot;
    protected CommandData commandData;

    public Command(String name, String description) {
        this.commandData = new CommandData(name, description);
    }

    public abstract void execute(SlashCommandEvent var1);
}

