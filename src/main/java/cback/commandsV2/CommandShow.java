package cback.commandsV2;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class CommandShow extends Command {
    public CommandShow() {
        super("show", "Add or remove a show from the database");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        long time = System.currentTimeMillis();
        event.reply("Show!").setEphemeral(true) // reply or acknowledge
                .flatMap(v ->
                        event.getHook().editOriginal("This is gonna be an add show command!") // then edit original
                ).queue(); // Queue both reply and edit
        System.out.println("executed");
    }
}

