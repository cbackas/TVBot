package cback;

import com.google.gson.JsonSyntaxException;
import org.apache.http.message.BasicNameValuePair;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordClientImpl;
import sx.blah.discord.api.internal.DiscordEndpoints;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.responses.UserResponse;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;

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

    public static IMessage sendBufferedMessage(IChannel channel, String message) {
        RequestBuffer.RequestFuture<IMessage> sentMessage = RequestBuffer.request(() -> {
            try {
                return channel.sendMessage(message);
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
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
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
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
                    } catch (MissingPermissionsException e) {
                        e.printStackTrace();
                    } catch (DiscordException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        channel.getMessages().bulkDelete(toDelete);
                    } catch (DiscordException e) {
                        e.printStackTrace();
                    } catch (MissingPermissionsException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    public static void botLog(IMessage message) {
        try {
            String text = "@" + message.getAuthor().getDisplayName(message.getGuild()) + " issued ``" + message.getContent() + "`` in " + message.getChannel().mention();

            List<IUser> mentionsU = message.getMentions();
            List<IRole> mentionsG = message.getRoleMentions();
            for (IUser u : mentionsU) {
                String displayName = "\\@" + u.getDisplayName(message.getGuild());
                text.replace(u.mention(false), displayName).replace(u.mention(true), displayName);
            }
            for (IRole g : mentionsG) {
                String displayName = "\\@" + g.getName();
                text.replace(g.mention(), displayName).replace(g.mention(), displayName);
            }

            Util.sendMessage(TVBot.getInstance().getClient().getChannelByID(TVBot.BOTLOG_CHANNEL_ID), text);
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
            Util.sendPrivateMessage(TVBot.getInstance().getClient().getUserByID("73416411443113984"), text + " in ``#" + message.getChannel().getName() + "``");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendPrivateMessage(IUser user, String message) {
        try {
            user.getClient().getOrCreatePMChannel(user).sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                return DiscordUtils.getUserFromJSON(client, DiscordUtils.GSON.fromJson(result, UserResponse.class)).getName();
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

}
