package cback.eventFunctions;

import cback.Channels;
import cback.TVBot;
import cback.Util;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;

import java.time.Instant;

public class MessageChange {
    private TVBot bot;

    public MessageChange(TVBot bot) {
        this.bot = bot;
    }

    public void messageDeleted(GuildMessageDeleteEvent event) {
        if (event.getGuild().getId().equals(TVBot.getHomeGuild().getId()) && event.getMessage() != null) {
            if (!event.getAuthor().isBot() && !TVBot.messageCache.contains(event.getMessageID())) {
                if (event.getChannel().equals(Channels.DEV_CH_ID.getChannel())) {
                    TVBot.messageCache.remove(event.getMessageIdLong());
                    Message message = event.getMessage();
                    String content = message.getContentDisplay();
                    User author = event.getAuthor();
                    TextChannel channel = event.getChannel();

                    if (content.contains(":spoiler:")) return; // ignore spoilerbot caused stuff

                    Boolean tripped = true;
                    for (String p : bot.prefixes) {
                        if (content.startsWith(p)) {
                            tripped = false;
                        }
                    }

                    if (tripped) {
                        EmbedBuilder bld = new EmbedBuilder().setColor(java.awt.Color.decode("#ED4337"));
                        bld
                                .setAuthor(author.getName() + "#" + author.getDiscriminator(), null, Util.getAvatar(author))
                                .setDescription("**Message sent by **" + author.getAsMention() + "** deleted in **" + channel.getAsMention() + "\n" + message.getContent())
                                .setFooter("User ID: " + author.getStringID(), null)
                                .setTimestamp(Instant.now());

                        TextChannel MESSAGE_LOGS = event.getGuild().getTextChannelById(Channels.MESSAGELOG_CH_ID.getId());
                        Util.sendEmbed(MESSAGE_LOGS, bld.build());
                    }
                }
            }
        }
    }

    public void messageEdited(MessageUpdateEvent event) {
        /*if (event instanceof MessagePinEvent || event instanceof MessageUnpinEvent || event instanceof MessageEmbedEvent) {
            return;
        }*/

        if (event.getGuild().getId().equals(TVBot.getHomeGuild().getId()) && event.getMessage() != null) {
            if (!event.getAuthor().isBot()) {
                Message message = event.getMessage();
                Message oldMessage = event.getOldMessage();
                Message newMessage = event.getNewMessage();
                User author = event.getAuthor();
                TextChannel channel = event.getTextChannel();

                EmbedBuilder bld = new EmbedBuilder().setColor(java.awt.Color.decode("#FFA500"));
                bld
                        .setAuthor(author.getName() + "#" + author.getDiscriminator(), null, Util.getAvatar(author))
                        .setDescription("**Message Edited in **" + channel.getAsMention())
                        .addField("Before", oldMessage.getContentRaw(), false)
                        .addField("After", newMessage.getContentRaw(), false)
                        .setFooter("ID: " + message.getId(), null)
                        .setTimestamp(Instant.now());

                TextChannel MESSAGE_LOGS = event.getGuild().getTextChannelById(Channels.MESSAGELOG_CH_ID.getId());
                Util.sendEmbed(MESSAGE_LOGS, bld.build());
                bot.censorMessages(message);
            }
        }
    }

    public void nicknameChange(GuildMemberNickChangeEvent event) {
        if (event.getGuild().getId().equals(TVBot.getHomeGuild().getId())) {
            User user = event.getUser();

            String oldName = event.getUser().getName();
            if (event.getPrevNick().isBlank()) {
                oldName = event.getPrevNick();
            }

            String newName = event.getUser().getName();
            if (event.getNewNick().isBlank()) {
                newName = event.getNewNick();
            }

            EmbedBuilder bld = new EmbedBuilder().setColor(java.awt.Color.decode("#FFA500"));
            bld
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(), null, Util.getAvatar(user))
                    .setDescription(user.getAsMention() + " **nickname changed**")
                    .addField("Before", oldName, false)
                    .addField("After", newName, false)
                    .setFooter("ID: " + user.getId(), null)
                    .setTimestamp(Instant.now());

            TextChannel MESSAGE_LOGS = event.getGuild().getTextChannelById(Channels.MESSAGELOG_CH_ID.getId());
            Util.sendEmbed(MESSAGE_LOGS, bld.build());
        }
    }
}