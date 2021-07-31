package cback;

//import cback.commands.Command;

import com.jagrosh.jdautilities.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Util {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("^<@!?(\\d+)>$");
    private static Color BOT_COLOR = Color.decode("#" + TVBot.getInstance().getConfigManager().getConfigValue("bot_color"));
    private static org.slf4j.Logger logger = LoggerFactory.getLogger("TVBot");

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
        TextChannel errorChannel = Channels.ERRORLOG_CH_ID.getChannel();

        StringBuilder stack = new StringBuilder();
        for (StackTraceElement s : e.getStackTrace()) {
            stack.append(s.toString());
            stack.append("\n");
        }
        String stackString = stack.toString();
        if (stackString.length() > 800) {
            stackString = stackString.substring(0, 800);
        }

        EmbedBuilder bld = new EmbedBuilder().setColor(Color.ORANGE).setTimestamp(Instant.now());

        if (message != null) {
            bld.setAuthor(message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(), message.getAuthor().getEffectiveAvatarUrl()).appendDescription(message.getContentRaw()).addBlankField(false);
        }

        bld.appendDescription(text).addField("Exception:", e.toString(), false).addField("Stack:", stackString, false);

        try {
            errorChannel.sendMessage(bld.build()).queue();
        } catch (Exception ex) {
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
            stackString = stackString.substring(0, 800);
        }

        Channels.ERRORLOG_CH_ID.getChannel().sendMessage(bld.setColor(BOT_COLOR).setTimestamp(Instant.now()).setAuthor(message.getAuthor().getName() + '#' + message.getAuthor().getDiscriminator(), null, message.getAuthor().getAvatarUrl()).setDescription(message.getContentRaw()).addField("\u200B", "\u200B", false).addField("Exeption:", e.toString(), false).addField("Stack:", stackString, false).build()).queue();
    }

    public static void reportHome(Exception e) {
        e.printStackTrace();
        EmbedBuilder bld = new EmbedBuilder();

        Channels.ERRORLOG_CH_ID.getChannel().sendMessage(bld.setColor(BOT_COLOR).setTimestamp(Instant.now()).addField("Exeption:", e.toString(), false).build()).queue();
    }

    /**
     * Send botLog
     */
    public static void botLog(Message message) {
        try {
            Channels.BOTLOG_CH_ID.getChannel().sendMessage(new EmbedBuilder().setColor(BOT_COLOR).setAuthor(message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator(), null, null).setDescription(message.getContentDisplay()).setFooter(message.getGuild().getName() + "/#" + message.getChannel().getName(), null).setTimestamp(Instant.now()).build()).queue();
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Command syntax error
     */
    public static void syntaxError(Command command, Message message) {
        try {

            EmbedBuilder bld =
                    new EmbedBuilder()
                            .setColor(BOT_COLOR)
                            .setAuthor(command.getName())
                            .setDescription(command.getHelp())
                            .addField("Syntax:", TVBot.COMMAND_PREFIX + command.getArguments(), false);

            sendEmbed(message.getChannel(), bld.build());
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Add a server log
     */
    public static void sendLog(Message message, String text) {
        try {
            EmbedBuilder embed =
                    new EmbedBuilder().setFooter("Action by @" + message.getAuthor().getName(), message.getAuthor().getEffectiveAvatarUrl()).setDescription(text).setColor(Color.GRAY).setTimestamp(Instant.now());

            Channels.SERVERLOG_CH_ID.getChannel().sendMessage(embed.build()).queue();
        } catch (Exception e) {
            reportHome(e);
        }
    }

    public static void sendLog(Message message, String text, Color color) {
        try {
            EmbedBuilder embed =
                    new EmbedBuilder().setFooter("Action by @" + message.getAuthor().getName(), message.getAuthor().getEffectiveAvatarUrl()).setDescription(text).setColor(color).setTimestamp(Instant.now());

            Channels.SERVERLOG_CH_ID.getChannel().sendMessage(embed.build()).queue();
        } catch (Exception e) {
            reportHome(e);
        }
    }

    /**
     * Synchronous simple embed
     *
     * @param channel
     * @param message
     * @return
     */
    public static Message simpleEmbedSync(TextChannel channel, String message) {

        try {
            MessageEmbed embed = new EmbedBuilder().appendDescription(message).setColor(Color.ORANGE).build();
            return channel.sendMessage(embed).complete();
        } catch (Exception ex) {
            Util.getLogger().error("Failed to send Embed!");
            ex.printStackTrace();
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
        }
        return null;
    }

    /**
     * Send simple fast embeds
     */
    public static void simpleEmbed(TextChannel channel, String message) {

        try {
            MessageEmbed embed = new EmbedBuilder().appendDescription(message).setColor(Color.ORANGE).build();
            channel.sendMessage(embed).queue();
        } catch (Exception ex) {
            Util.getLogger().error("Failed to send Embed!");
            ex.printStackTrace();
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
        }
    }

    public static void simpleEmbed(TextChannel channel, String message, Color color) {
        try {
            MessageEmbed embed = new EmbedBuilder().appendDescription(message).setColor(color).build();
            channel.sendMessage(embed).queue();
        } catch (Exception ex) {
            Util.getLogger().error("Failed to send Embed");
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
        }
    }

    public static void sendEmbed(MessageChannel channel, MessageEmbed embed) {
        try {
            channel.sendMessage(embed).queue();
        } catch (Exception ex) {
            Util.getLogger().error("Failed to send Embed");
            ex.printStackTrace();
            reportHome("Embed failed to send in " + channel.getName(), ex, null);
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
    public static void sendPrivateMessage(User user, String message) {
        try {
            user.openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(message).queue());
        } catch (Exception e) {
            reportHome(e);
        }
    }

    public static void sendPrivateEmbed(User user, String message) {
        try {
            user.openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(message).queue());
            //simpleEmbed(pmChannel, message);
        } catch (Exception e) {
            reportHome(e);
        }
    }

    //EMBEDBUILDER STUFF
    private static String[] defaults =
            {"6debd47ed13483642cf09e832ed0bc1b", "322c936a8c8be1b803cd94861bdfa868", "dd4dbc0016779df1378e7812eabaa04d", "0e291f67c9274a1abdddeb3fd919cbaa", "1cbd08c76f8af6dddce02c5138971129"};

    public static EmbedBuilder getEmbed() {
        return new EmbedBuilder().setAuthor(TVBot.getInstance().getClient().getSelfUser().getName(), "https://github.com/cbackas/", TVBot.getInstance().getClient().getSelfUser().getEffectiveAvatarUrl());
    }

    public static String getTag(User user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    public static EmbedBuilder getEmbed(User user) {
        return getEmbed().setFooter("Requested by @" + getTag(user), getAvatar(user));
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

    public static User getUserFromMentionArg(String arg) {
        Matcher matcher = USER_MENTION_PATTERN.matcher(arg);
        if (matcher.matches()) {
            return TVBot.getInstance().getClient().getUserById(matcher.group(1));
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
        //TODO does this even work? need to test !rule or whatever
        try {
            return TVBot.getInstance().getClient().getTextChannelById(263184364811059200L).retrieveMessageById(ruleID).complete().toString();
        } catch (Exception e) {
            reportHome(e);
        }
        return null;
    }

    /**
     * Sets the lounge's security level
     */
    public static void setSecurity() {
        //TODO this does nothing? -troy
        try {
            Guild lounge = TVBot.getInstance().getHomeGuild();
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

            EmbedBuilder bld =
                    new EmbedBuilder().setAuthor(author.getName() + '#' + author.getDiscriminator(), null, author.getEffectiveAvatarUrl()).setDescription(message.getContentRaw()).setTimestamp(Instant.now());

            for (Message.Attachment a : message.getAttachments()) {
                bld.setImage(a.getUrl());
            }

            if (type == 1) {
                bld.setFooter(author.getId(), null).setColor(getBotColor());
            } else if (type == 2) {
                bld.setFooter("in #" + message.getChannel().getName(), null).setColor(Color.orange);
            }

            return bld.build();
        } catch (Exception e) {
            reportHome(message, e);
            return null;
        }
    }

    public static String[] splitArgs(String args, int limit) {
        if (args.isEmpty() || StringUtils.isWhitespace(args)) {
            return new String[0];
        } else {
            return args.split("\\s+", limit);
        }
    }

    public static String[] splitArgs(String args) {
        if (args.isEmpty() || StringUtils.isWhitespace(args)) {
            return new String[0];
        } else {
            return args.split("\\s+");
        }
    }

    public static List<GuildChannel> getPermChannels(Guild guild) {
        var staff = guild.getCategoryById(TVBot.STAFF_CAT_ID);
        var info = guild.getCategoryById(TVBot.INFO_CAT_ID);
        var disc = guild.getCategoryById(TVBot.DISCUSSION_CAT_ID);
        var fun = guild.getCategoryById(TVBot.FUN_CAT_ID);
        var cards = guild.getCategoryById(TVBot.CARDS_CAT_ID);
        var newly = guild.getCategoryById(TVBot.NEW_CAT_ID);

        List<GuildChannel> permChannels = new ArrayList<>();
        permChannels.addAll(staff.getChannels());
        permChannels.addAll(info.getChannels());
        permChannels.addAll(disc.getChannels());
        permChannels.addAll(fun.getChannels());
        permChannels.addAll(cards.getChannels());
        permChannels.addAll(newly.getChannels());

        return permChannels;
    }

    public static org.slf4j.Logger getLogger() {
        return logger;
    }
}