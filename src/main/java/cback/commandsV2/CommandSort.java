package cback.commandsV2;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class CommandSort extends Command {
    public CommandSort() {
        super();
        this.commandData = new CommandData("sort", "Sorts text channels into alphebetic categories");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        event.reply("Sort!")
                .setEphemeral(true)
                .flatMap(v ->
                        event.getHook().editOriginal("We be sortin here")
                )
                .queue();
    }
}
