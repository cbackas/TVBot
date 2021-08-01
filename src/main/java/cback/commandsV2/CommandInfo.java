package cback.commandsV2;

import cback.Util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CommandInfo extends Command {
    public CommandInfo() {
        super();
        this.commandData = new CommandData("info", "Displays some statistics about the server and the bot");
    }

    @Override
    public void execute(SlashCommandEvent event) {
        MessageEmbed loadingEmbed = new EmbedBuilder().setColor(Util.getBotColor())
                .setDescription("...")
                .build();

        event.replyEmbeds(loadingEmbed)
                .setEphemeral(true)
                .flatMap(v -> {
                    int userCount = event.getGuild().getMemberCount();
                    List<Member> boosters = event.getGuild().getBoosters();
                    System.out.println(boosters);
                    int channelCount = event.getGuild().getChannels().size();
                    OffsetDateTime serverCreatedDateTime = event.getGuild().getTimeCreated();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                    MessageEmbed infoEmbed = new EmbedBuilder()
                            .setColor(Util.getBotColor())
                            .setTitle(event.getGuild().getName())
                            .setThumbnail(event.getGuild().getIconUrl())
                            .addField("Created: ", serverCreatedDateTime.format(formatter), true)
                            .addField("Members: ", Integer.toString(userCount), true)
                            .addField("Channels: ", String.valueOf(channelCount), true)
                            .addField("Server Boosters: ", boosters.stream().map(Member::getNickname).collect(Collectors.joining("\n")), true)
                            .addField("Source Code: ", "[`GitHub`](https://github.com/cbackas/TVBot)", true)
                            .build();

                    return event.getHook().editOriginalEmbeds(infoEmbed);
                })
                .queue();
    }
}
