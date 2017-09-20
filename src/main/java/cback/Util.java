package cback;

import cback.commands.Command;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Util {
    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("^<@!?(\\d+)>$");

    static IDiscordClient client = TVBot.getClient();
    static ConfigManager cm = TVBot.getConfigManager();
    static Color BOT_COLOR = Color.decode("#" + cm.getConfigValue("bot_color"));

    /**
     * Returns the bot's color as a Color object
     */
    public static Color getBotColor() {
        return BOT_COLOR;
    }

    public static void sendMessage(IChannel channel, String message) {
        try {
            channel.sendMessage(message);
        } catch (Exception e) {
            reportHome(e);
        }
    }

    /**
     * Send report
     */
    public static void reportHome(IMessage message, Exception e) {
        e.printStackTrace();

        IChannel errorChannel = client.getChannelByID(TVBot.ERRORLOG_CH_ID);

        EmbedBuilder bld = new EmbedBuilder()
                .withColor(BOT_COLOR)
                .withTimestamp(System.currentTimeMillis())
                .withAuthorName(message.getAuthor().getName() + '#' + message.getAuthor().getDiscriminator())
                .withAuthorIcon(getAvatar(message.getAuthor()))
                .withDesc(message.getContent())
                .appendField("\u200B", "\u200B", false)

                .appendField("Exeption:", e.toString(), false);

        StringBuilder stack = new StringBuilder();
        for (StackTraceElement s : e.getStackTrace()) {
            stack.append(s.toString());
            stack.append("\n");
        }

        String stackString = stack.toString();
        if (stackString.length() > 1024) {
            stackString = stackString.substring(0, 1800);
        }

        bld
                .appendField("Stack:", stackString, false);

        sendEmbed(errorChannel, bld.build());
    }

    public static void reportHome(Exception e) {
        e.printStackTrace();

        IChannel errorChannel = client.getChannelByID(TVBot.ERRORLOG_CH_ID);

        EmbedBuilder bld = new EmbedBuilder()
                .withColor(BOT_COLOR)
                .withTimestamp(System.currentTimeMillis())
                .appendField("Exeption:", e.toString(), false);

        StringBuilder stack = new StringBuilder();
        for (StackTraceElement s : e.getStackTrace()) {
            stack.append(s.toString());
            stack.append("\n");
        }

        String stackString = stack.toString();
        if (stackString.length() > 1024) {
            stackString = stackString.substring(0, 1800);
        }

        bld
                .appendField("Stack:", stackString, false);

        sendEmbed(errorChannel, bld.build());
    }

    /**
     * Send botLog
     */
    public static void botLog(IMessage message) {
        try {
            IChannel botLogChannel = client.getChannelByID(TVBot.BOTLOG_CH_ID);

            EmbedBuilder bld = new EmbedBuilder()
                    .withColor(BOT_COLOR)
                    .withAuthorName(message.getAuthor().getName() + '#' + message.getAuthor().getDiscriminator())
                    .withAuthorIcon(getAvatar(message.getAuthor()))
                    .withDesc(message.getFormattedContent())
                    .withFooterText(message.getGuild().getName() + "/#" + message.getChannel().getName())
                    .withTimestamp(System.currentTimeMillis());

            sendEmbed(botLogChannel, bld.build());
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Command syntax error
     */
    public static void syntaxError(Command command, IMessage message) {
        try {
            EmbedBuilder bld = new EmbedBuilder()
                    .withColor(BOT_COLOR)
                    .withAuthorName(command.getName())
                    .withAuthorIcon(TVBot.getClient().getApplicationIconURL())
                    .withDesc(command.getDescription())
                    .appendField("Syntax:", TVBot.getPrefix() + command.getSyntax(), false);

            sendEmbed(message.getChannel(), bld.build());
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Delete a message
     */
    public static void deleteMessage(IMessage message) {
        try {
            message.delete();
        } catch (Exception e) {
            reportHome(message, e);
        }
    }

    /**
     * Add a server log
     */
    public static IMessage sendLog(IMessage message, String text) {
        RequestBuffer.RequestFuture<IMessage> future = RequestBuffer.request(() -> {
            try {
                IUser user = message.getAuthor();

                new EmbedBuilder();
                EmbedBuilder embed = new EmbedBuilder();

                embed.withFooterIcon(getAvatar(user));
                embed.withFooterText("Action by @" + getTag(user));

                embed.withDescription(text);

                embed.withTimestamp(System.currentTimeMillis());

                IDiscordClient client = TVBot.getInstance().getClient();
                return new MessageBuilder(client).withEmbed(embed.withColor(Color.GRAY).build())
                        .withChannel(TVBot.SERVERLOG_CH_ID).send();
            } catch (MissingPermissionsException | DiscordException e) {
                reportHome(e);
            }
            return null;
        });
        return future.get();
    }

    public static IMessage sendLog(IMessage message, String text, Color color) {
        RequestBuffer.RequestFuture<IMessage> future = RequestBuffer.request(() -> {
            try {
                IUser user = message.getAuthor();

                new EmbedBuilder();
                EmbedBuilder embed = new EmbedBuilder();

                embed.withFooterIcon(getAvatar(user));
                embed.withFooterText("Action by @" + getTag(user));

                embed.withDescription(text);

                embed.withTimestamp(System.currentTimeMillis());

                IDiscordClient client = TVBot.getInstance().getClient();
                return new MessageBuilder(client).withEmbed(embed.withColor(color).build())
                        .withChannel(TVBot.SERVERLOG_CH_ID).send();
            } catch (MissingPermissionsException | DiscordException e) {
                reportHome(e);
            }
            return null;
        });
        return future.get();
    }

    /**
     * Send simple fast embeds
     */
    public static void simpleEmbed(IChannel channel, String message) {
        sendEmbed(channel, new EmbedBuilder().withDescription(message).withColor(BOT_COLOR).build());
    }

    public static void simpleEmbed(IChannel channel, String message, Color color) {
        sendEmbed(channel, new EmbedBuilder().withDescription(message).withColor(color).build());
    }

    public static IMessage sendEmbed(IChannel channel, EmbedObject embedObject) {
        RequestBuffer.RequestFuture<IMessage> future = RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(TVBot.getInstance().getClient()).withEmbed(embedObject)
                        .withChannel(channel).send();
            } catch (MissingPermissionsException | DiscordException e) {
                reportHome(e);
            }
            return null;
        });
        return future.get();
    }

    public static IMessage sendBufferedMessage(IChannel channel, String message) {
        RequestBuffer.RequestFuture<IMessage> sentMessage = RequestBuffer.request(() -> {
            try {
                return channel.sendMessage(message);
            } catch (MissingPermissionsException | DiscordException e) {
                e.printStackTrace();
            }
            return null;
        });
        return sentMessage.get();
    }

    public static void deleteBufferedMessage(IMessage message) {
        RequestBuffer.request(() -> {
            try {
                message.delete();
            } catch (MissingPermissionsException | DiscordException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Bulk deletes a list of messages
     */
    public static void bulkDelete(IChannel channel, List<IMessage> toDelete) {
        RequestBuffer.request(() -> {
            if (toDelete.size() > 0) {
                if (toDelete.size() == 1) {
                    try {
                        toDelete.get(0).delete();
                    } catch (MissingPermissionsException | DiscordException e) {
                        reportHome(e);
                    }
                } else {
                    try {
                        channel.bulkDelete(toDelete);
                    } catch (DiscordException | MissingPermissionsException e) {
                        reportHome(e);
                    }

                }
            }
        });
    }

    /**
     * Sends an announcement (message in general and announcements)
     */
    public static void sendAnnouncement(String message) {
        Util.sendMessage(TVBot.getInstance().getClient().getChannelByID(TVBot.GENERAL_CH_ID), message);
        Util.sendMessage(TVBot.getInstance().getClient().getChannelByID(TVBot.ANNOUNCEMENT_CH_ID), message);
    }

    public static void sendPrivateMessage(IUser user, String message) {
        try {
            user.getClient().getOrCreatePMChannel(user).sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
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
                .withAuthorIcon(getAvatar(TVBot.getInstance().getClient().getOurUser()))
                .withAuthorUrl("https://github.com/cbackas/")
                .withAuthorName(getTag(TVBot.getInstance().getClient().getOurUser()));
    }

    public static String getTag(IUser user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    public static EmbedBuilder getEmbed(IUser user) {
        return getEmbed().withFooterIcon(getAvatar(user))
                .withFooterText("Requested by @" + getTag(user));
    }

    public static String getAvatar(IUser user) {
        return user.getAvatar() != null ? user.getAvatarURL() : getDefaultAvatar(user);
    }

    public static String getDefaultAvatar(IUser user) {
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

    public static IUser getUserFromMentionArg(String arg) {
        Matcher matcher = USER_MENTION_PATTERN.matcher(arg);
        if (matcher.matches()) {
            return TVBot.getInstance().getClient().getUserByID(Long.parseLong(matcher.group(1)));
        }
        return null;
    }

    public static String to12Hour(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            Date dateObj = sdf.parse(time);
            return new SimpleDateFormat("K:mm").format(dateObj);
        } catch (Exception e) {
            reportHome(e);
        }
        return time;
    }

    public static String getRule(Long ruleID) {
        try {
            String rule = TVBot.getInstance().getClient().getChannelByID(263184364811059200l).getMessageByID(ruleID).getContent();

            return rule;
        } catch (Exception e) {
            reportHome(e);
        }
        return null;
    }
}
