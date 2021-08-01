package cback.commandsV2;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class CommandPing extends Command {
    public CommandPing() {
        super("ping", "Gives you latency info duh");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                .flatMap(v ->
                        event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                ).queue(); // Queue both reply and edit
        System.out.println("executed");
    }
}
