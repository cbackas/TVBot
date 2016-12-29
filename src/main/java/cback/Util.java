package cback;

import com.google.gson.JsonSyntaxException;
import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import org.apache.http.message.BasicNameValuePair;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.api.internal.json.objects.UserObject;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Util {

    public static File botPath;

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("^<@!?(\\d+)>$");

    static {
        try {
            botPath = new File(TVBot.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(IChannel channel, String message) {
        try {
            channel.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IMessage sendEmbed(IChannel channel, EmbedObject embedObject) {
        RequestBuffer.RequestFuture<IMessage> future = RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(TVBot.getInstance().getClient()).withEmbed(embedObject)
                        .withChannel(channel).withContent("\u200B").send();
            } catch (Exception e) {
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

    public static void deleteMessage(IMessage message) {
        try {
            message.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static void bulkDelete(IChannel channel, List<IMessage> toDelete) {
        RequestBuffer.request(() -> {
            if (toDelete.size() > 0) {
                if (toDelete.size() == 1) {
                    try {
                        toDelete.get(0).delete();
                    } catch (MissingPermissionsException | DiscordException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        channel.getMessages().bulkDelete(toDelete);
                    } catch (DiscordException | MissingPermissionsException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    public static void botLog(IMessage message) {
        IDiscordClient client = TVBot.getInstance().getClient();
        try {
            String text = "@" + message.getAuthor().getDisplayName(message.getGuild()) + " issued ``" + message.getFormattedContent() + "`` in " + message.getGuild().getName() + "/" + message.getChannel().mention();

            Util.sendWebhook(TVBot.BOTLOG_WEBHOOK_URL, client.getApplicationIconURL(), client.getApplicationName(), text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendAnnouncement(String message) {
        try {
            Util.sendMessage(TVBot.getInstance().getClient().getChannelByID(TVBot.GENERAL_CHANNEL_ID), message);
            Util.sendMessage(TVBot.getInstance().getClient().getChannelByID(TVBot.ANNOUNCEMENT_CHANNEL_ID), message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void errorLog(IMessage message, String text) {
        try {
            Util.sendPrivateMessage(TVBot.getInstance().getClient().getUserByID("73416411443113984"), text + " in  " + message.getChannel().mention());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Boolean permissionCheck(IMessage message, String role) {
        try {
            return message.getGuild().getRolesForUser(message.getAuthor()).contains(message.getGuild().getRolesByName(role).get(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendPrivateMessage(IUser user, String message) {
        try {
            user.getClient().getOrCreatePMChannel(user).sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendWebhook(String URL, String iconURL, String displayName, String message) {
        try {
            new Slack(URL)
                    .icon(iconURL)
                    .displayName(displayName)
                    .push(new SlackMessage(message));
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
                .withAuthorUrl("https://github.com/ArsenArsen/FlareBot")
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
            return TVBot.getInstance().getClient().getUserByID(matcher.group(1));
        }
        return null;
    }

    public static String to12Hour(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            Date dateObj = sdf.parse(time);
            return new SimpleDateFormat("K:mm").format(dateObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static String requestUsernameByID(String id) {
        IDiscordClient client = TVBot.getInstance().getClient();

        RequestBuffer.RequestFuture<String> userNameResult = RequestBuffer.request(() -> {
            try {
                String result = ((DiscordClientImpl) client).REQUESTS.GET.makeRequest(DiscordEndpoints.USERS + id,
                        new BasicNameValuePair("authorization", TVBot.getInstance().getClient().getToken()));
                return DiscordUtils.GSON.fromJson(result, UserObject.class).username;
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            }

            return "NULL";
        });

        return userNameResult.get();
    }

    public static List<IUser> getUsersByRole(String roleID) {
        try {
            IGuild guild = TVBot.getInstance().getClient().getGuildByID("192441520178200577");
            IRole role = guild.getRoleByID(roleID);

            if (role != null) {
                List<IUser> allUsers = guild.getUsers();
                List<IUser> ourUsers = new ArrayList<>();


                for (IUser u : allUsers) {
                    List<IRole> userRoles = u.getRolesForGuild(guild);

                    if (userRoles.contains(role)) {
                        ourUsers.add(u);
                    }
                }

                return ourUsers;

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<IMessage> getSuggestions() {
        try {
            IChannel channel = TVBot.getInstance().getClient().getGuildByID("192441520178200577").getChannelByID("192444470942236672");

            List<IMessage> messages = channel.getPinnedMessages();
            List<IMessage> permM = Arrays.asList(channel.getMessageByID("228166713521340416"), channel.getMessageByID("236703936789217285"), channel.getMessageByID("246306837748514826"));
            permM.forEach(messages::remove);

            return messages;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRule(String ruleID) {
        try {
            String rule = TVBot.getInstance().getClient().getChannelByID("251916332747063296").getMessageByID(ruleID).getContent();

            return rule;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
