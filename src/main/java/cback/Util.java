package cback;

//import cback.commands.Command;

import com.jagrosh.jdautilities.command.Command;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Util {
    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("^<@!?(\\d+)>$");

    static JDAClient client = TVBot.getClient();
    static ConfigManager cm = TVBot.getConfigManager();
    static Color BOT_COLOR = Color.decode("#" + cm.getConfigValue("bot_color"));

    /**
     * Returns the bot's color as a Color object
     */
    public static Color getBotColor() {
        return BOT_COLOR;
    }

    public static void sendMessage(MessageChannel channel, String message) {
        try {
            channel.sendMessage(message).queue();
        } catch (Exception e) {
            reportHome("Message failed to send in " + channel.getName(), e, null);
        }
    }

    /**
     * Delete a message
     */
    public static void deleteMessage(Message message) {
        try {
            message.delete().queue();
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Send report
     */

    public static void reportHome(String text, Exception e, Message message) {
        TextChannel errorChannel = Channels.TEST_CH_ID.getChannel();

        StringBuilder stack = new StringBuilder();
        for(StackTraceElement s : e.getStackTrace()) {
            stack.append(s.toString());
            stack.append("\n");
        }
        String stackString = stack.toString();
        if(stackString.length() > 800) {
            stackString = stackString.substring(0, 800);
        }

        EmbedBuilder bld = new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setTimestamp(Instant.now());

        if(message != null) {
            bld
                    .setAuthor(message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(), message.getAuthor().getEffectiveAvatarUrl())
                    .appendDescription(message.getContentRaw())
                    .addBlankField(false);
        }

        bld
                .appendDescription(text)
                .addField("Exception:", e.toString(), false)
                .addField("Stack:", stackString, false);

        try {
            errorChannel.sendMessage(bld.build()).queue();
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void reportHome(Message message, Exception e) {
        e.printStackTrace();
        EmbedBuilder bld = new EmbedBuilder();

        StringBuilder stack = new StringBuilder();
        for (StackTraceElement s : e.getStackTrace()) {
            stack.append(s.toString());
            stack.append("\n");
        }

        String stackString = stack.toString();
        if (stackString.length() > 1024) {
            stackString = stackString.substring(0, 1800);
        }

        Channels.ERRORLOG_CH_ID.getChannel().sendMessage(bld
                .setColor(BOT_COLOR)
                .setTimestamp(Instant.now())
                .setAuthor(message.getAuthor().getName() + '#' + message.getAuthor().getDiscriminator(), null, message.getAuthor().getAvatarUrl())
                .setDescription(message.getContentRaw())
                .addField("\u200B", "\u200B", false)
                .addField("Exeption:", e.toString(), false)
                .addField("Stack:", stackString, false)
                .build()).queue();
    }

    public static void reportHome(Exception e) {
        e.printStackTrace();
        EmbedBuilder bld = new EmbedBuilder();

        StringBuilder stack = new StringBuilder();
        for (StackTraceElement s : e.getStackTrace()) {
            stack.append(s.toString());
            stack.append("\n");
        }

        String stackString = stack.toString();
        if (stackString.length() > 1024) {
            stackString = stackString.substring(0, 1800);
        }

        Channels.ERRORLOG_CH_ID.getChannel().sendMessage(bld
                .setColor(BOT_COLOR)
                .setTimestamp(Instant.now())
                .addField("Exeption:", e.toString(), false)
                .addField("Stack:", stackString, false)
                .build()).queue();
    }

    /**
     * Send botLog
     */
    public static void botLog(Message message) {
        try {
            Channels.BOTLOG_CH_ID.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(BOT_COLOR)
                    .setAuthor(message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(), null, null)
                    .setDescription(message.getContentDisplay())
                    .setFooter(message.getGuild().getName() + "/#" + message.getChannel().getName(), null)
                    .setTimestamp(Instant.now())
                    .build()).queue();
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Command syntax error
     */
    public static void syntaxError(Command command, Message message) {
        try {

            EmbedBuilder bld = new EmbedBuilder()
                    .setColor(BOT_COLOR)
                    .setAuthor(command.getName(), TVBot.getClient().getApplicationById("583042681496797198").complete().getIconUrl())
                    .setDescription(command.getHelp())
                    .addField("Syntax:", TVBot.getPrefix() + command.getName(), false);

            sendEmbed(message.getChannel(), bld.build());
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Add a server log
     */
    public static MessageAction sendLog(Message message, String text) {
            try {

                new EmbedBuilder();
                EmbedBuilder embed = new EmbedBuilder();

                embed.setFooter("Action by @" + message.getAuthor().getName(), message.getAuthor().getEffectiveAvatarUrl());

                embed.setDescription(text);

                embed.setTimestamp(Instant.now());

                JDAClient client = TVBot.getClient();
                return new MessageBuilder().setEmbed(embed.setColor(Color.GRAY).build())
                        .sendTo(Channels.SERVERLOG_CH_ID.getChannel());
            } catch (Exception e) {
                reportHome(e);
            }
            return null;
    }

    public static MessageAction sendLog(Message message, String text, Color color) {
            try {
                User user = message.getAuthor();

                new EmbedBuilder();
                EmbedBuilder embed = new EmbedBuilder();

                embed.setFooter("Action by @" + getTag(user), getAvatar(user));
                embed.setDescription(text);

                embed.setTimestamp(Instant.now());

                JDAClient client = TVBot.getClient();
                return new MessageBuilder().setEmbed(embed.setColor(color).build())
                        .sendTo(Channels.SERVERLOG_CH_ID.getChannel());
            } catch (Exception e) {
                reportHome(e);
            }
            return null;
    }

    /**
     * Send simple fast embeds
     */
    public static Message simpleEmbed(MessageChannel channel, String message) {
        try {
            MessageEmbed embed = new EmbedBuilder().appendDescription(message).setColor(Color.ORANGE).build();
        } catch(Exception ex) {
            System.out.println("Failed to send Embed!");
            ex.printStackTrace();
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
        }
        return null;
    }

    public static void simpleEmbed(MessageChannel channel, String message, Color color) {
        try {
            MessageEmbed embed = new EmbedBuilder().appendDescription(message).setColor(color).build();
            channel.sendMessage(embed).queue();
        } catch(Exception ex) {
            System.out.println("Failed to send Embed");
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
        }
    }

    public static void sendEmbed(MessageChannel channel, MessageEmbed embed) {
        try {
            channel.sendMessage(embed).queue();
        } catch(Exception ex) {
            System.out.println("Failed to send Embed");
            ex.printStackTrace();
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
        }
    }

    public static MessageAction sendBufferedMessage(TextChannel channel, String message) {
        try {
            return channel.sendMessage(message);
        } catch (Exception e) {
            reportHome(e);
        }
        return null;
    }

    public static void deleteBufferedMessage(Message message) {
        try {
            message.delete().queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Bulk deletes a list of messages
     */
    public static void bulkDelete(TextChannel channel, List<Message> toDelete) {
        if (toDelete.size() > 0) {
            if (toDelete.size() == 1) {
                try {
                    toDelete.get(0).delete().queue();
                } catch (Exception e) {
                    reportHome(e);
                }
            } else {
                try {
                    channel.deleteMessages(toDelete).queue();
                } catch (Exception e) {
                    reportHome(e);
                }
            }
        }
    }

    /**
     * Sends an announcement (message in general and announcements)
     */
    public static void sendAnnouncement(String message) {
        Util.sendMessage(Channels.GENERAL_CH_ID.getChannel(), message);
        Util.sendMessage(Channels.ANNOUNCEMENT_CH_ID.getChannel(), message);
    }

    /**
     * Sending private messages
     */
    public static void sendPrivateMessage(Member user, String message) {
        try {
            user.openPrivateChannel().queue((privateChannel) ->
                    privateChannel.sendMessage(message).queue());
        } catch (Exception e) {
            reportHome(e);
        }
    }

    public static void sendPrivateEmbed(User user, String message) {
        try {
             user.openPrivateChannel().queue((privateChannel) ->
                    privateChannel.sendMessage(message).queue());
            //simpleEmbed(pmChannel, message);
        } catch (Exception e) {
            reportHome(e);
        }
    }

    //EMBEDBUILDER STUFF
    private static String[] defaults = {
            "6debd47ed13483642cf09e832ed0bc1b",
            "322c936a8c8be1b803cd94861bdfa868",
            "dd4dbc0016779df1378e7812eabaa04d",
            "0e291f67c9274a1abdddeb3fd919cbaa",
            "1cbd08c76f8af6dddce02c5138971129"
    };

    public static EmbedBuilder getEmbed() {
        return new EmbedBuilder()
                .setAuthor(client.getJDA().getSelfUser().getName(), "https://github.com/cbackas/", client.getJDA().getSelfUser().getEffectiveAvatarUrl());
    }

    public static String getTag(User user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    public static EmbedBuilder getEmbed(User user) {
        return getEmbed()
                .setFooter("Requested by @" + getTag(user), getAvatar(user));
    }

    public static String getAvatar(User user) {
        return user.getAvatarId() != null ? user.getAvatarUrl() : getDefaultAvatar(user);
    }

    public static String getDefaultAvatar(User user) {
        int discrim = Integer.parseInt(user.getDiscriminator());
        discrim %= defaults.length;
        return "https://discordapp.com/assets/" + defaults[discrim] + ".png";
    }

    //END EMBED BUILDER STUFF

    public static int toInt(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getCurrentTime() {
        return toInt(System.currentTimeMillis() / 1000);
    }

    public static Member getUserFromMentionArg(String arg) {
        Matcher matcher = USER_MENTION_PATTERN.matcher(arg);
        if (matcher.matches()) {
            return TVBot.getGuild().getMemberById(Long.parseLong(matcher.group(1)));
        }
        return null;
    }

    /**
     * Changes the time to a 12 hour format
     */
    public static String to12Hour(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            Date dateObj = sdf.parse(time);
            return new SimpleDateFormat("K:mm").format(dateObj);
        } catch (Exception e) {
            //reportHome(e);
        }
        return time;
    }

    /**
     * returns the string content of a rule, given the message ID of where it's found
     */
    public static String getRule(Long ruleID) {
        try {
            return TVBot.getClient().getJDA().getTextChannelById(263184364811059200L).getMessageById(ruleID).toString();
        } catch (Exception e) {
            reportHome(e);
        }
        return null;
    }

    /**
     * returns a count of mentions
     */
    public static int mentionsCount(String content) {
        String[] args = content.split(" ");
        if (args.length > 0) {
            int count = 0;
            for (String arg : args) {
                Matcher matcher = USER_MENTION_PATTERN.matcher(arg);
                if (matcher.matches()) {
                    count++;
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    /**
     * Sets the lounge's security level
     */
    public static void setSecurity() {
        try {
            Guild lounge = TVBot.getHomeGuild();
            lounge.getVerificationLevel();
        } catch (Exception e) {
            Util.reportHome(e);
        }
    }

    /**
     * Returns an embed object for a simple botpm
     */
    public static MessageEmbed buildBotPMEmbed(Message message, int type) {
        try {
            User author = message.getAuthor();

            EmbedBuilder bld = new EmbedBuilder()
                    .setAuthor(author.getName() + '#' + author.getDiscriminator(), null, author.getEffectiveAvatarUrl())
                    .setDescription(message.getContentRaw())
                    .setTimestamp(Instant.now());

            for (Message.Attachment a : message.getAttachments()) {
                bld.setImage(a.getUrl());
            }

            if (type == 1) {
                bld.setFooter(author.getId(), null)
                        .setColor(getBotColor());
            } else if (type == 2) {
                bld.setFooter("in #" + message.getChannel().getName(), null)
                        .setColor(Color.orange);
            }

            return bld.build();
        } catch (Exception e) {
            reportHome(message, e);
            return null;
        }
    }
}