package cback.commandsV2;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class CommandPing extends Command {
    public CommandPing() {
        super();
        this.commandData = new CommandData("ping", "Gives you latency info duh");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        long time = System.currentTimeMillis();
        event.reply("Pong!")
                .setEphemeral(true)
                .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
                .queue();
    }
}
