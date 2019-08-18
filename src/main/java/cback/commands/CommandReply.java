package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;

import java.awt.*;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandReply extends Command {

    private TVBot bot;

    public CommandReply(TVBot bot) {
        this.bot = bot;
        this.name = "reply";
    }
    @Override
    protected void execute(CommandEvent commandEvent) {
        String stuff = commandEvent.getMessage().getContentRaw().split(" ", 2)[1];

        Pattern pattern = Pattern.compile("^(\\d+) ?(.+)?");
        Matcher matcher = pattern.matcher(stuff);
        if(matcher.find()) {
            String user = matcher.group(1);
            String reply = matcher.group(2);

            if(reply != null) {
                Member replyTo = commandEvent.getGuild().getMemberById(Long.parseLong(user));
                Util.sendPrivateMessage(replyTo.getUser(), reply);

                EmbedBuilder bld = new EmbedBuilder()
                        .setAuthor("To: " + replyTo.getEffectiveName(), null, commandEvent.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(reply)
                        .setColor(Color.GREEN)
                        .setFooter("message sent", null)
                        .setTimestamp(Instant.now());

                Util.sendEmbed(commandEvent.getJDA().getTextChannelById(Channels.BOTPM_CH_ID.getId()), bld.build());
                Util.deleteMessage(commandEvent.getMessage());
            } else {
                Util.syntaxError(this, commandEvent.getMessage());
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
    }
}