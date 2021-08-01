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
                    List<Member> boostCount = event.getGuild().getBoosters();
                    int channelCount = event.getGuild().getChannels().size();
                    OffsetDateTime serverCreatedDateTime = event.getGuild().getTimeCreated();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(event.getGuild().getName());
                    embed.addField("Created: ", serverCreatedDateTime.format(formatter), true);
                    embed.addField("Members: ", Integer.toString(userCount), true);
                    embed.addField("Channels: ", String.valueOf(channelCount), true);
                    embed.addField("Server Boosters: ", boostCount.stream().map(Member::getNickname).collect(Collectors.joining("\n")), true);
                    embed.addBlankField(false);
                    embed.addField("Source Code: ", "[`GitHub`](https://github.com/cbackas/TVBot)", true);

                    MessageEmbed infoEmbed = embed.setColor(Util.getBotColor())
                            .build();

                    return event.getHook().editOriginalEmbeds(infoEmbed);
                })
                .queue();
    }
}
