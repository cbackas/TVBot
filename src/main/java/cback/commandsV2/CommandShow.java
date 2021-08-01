package cback.commandsV2;

import cback.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;

public class CommandShow extends Command {
    public CommandShow() {
        super();
        this.commandData = new CommandData("show", "Manage shows saved in the database");
        this.commandData.addSubcommands(new SubcommandData("add", "Add a show"));
        this.commandData.addSubcommands(new SubcommandData("remove", "Removes a show"));
    }

    @Override
    public void execute(SlashCommandEvent event) {
        MessageEmbed basicEmbed = new EmbedBuilder()
                .appendDescription("")
                .setColor(Util.getBotColor())
                .build();

        event.replyEmbeds(basicEmbed)
                .setEphemeral(false)
                .flatMap(v -> {
                    MessageEmbed responseEmbed = new EmbedBuilder()
                            .appendDescription(event.getSubcommandName())
                            .setColor(Util.getBotColor())
                            .build();


                    return event.getHook().editOriginalEmbeds(responseEmbed);
                })
                .queue();
    }
}

