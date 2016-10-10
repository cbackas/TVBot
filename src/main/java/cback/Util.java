package cback;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;


public class Util {

    public static File botPath;
    private static JSONObject config;

    static {
        try {
            botPath = new File(TVBot.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            org.json.simple.parser.JSONParser parser = new JSONParser();
            config = (JSONObject) parser.parse(new FileReader(botPath + "/TVproperties.json"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Optional<String> getToken(String fileName) {
        try {
            File tokenFile = new File(botPath, fileName);
            if (tokenFile.exists()) {
                String token = FileUtils.readFileToString(tokenFile, (String) null);
                if (!token.equalsIgnoreCase("TOKEN") && !token.isEmpty()) {
                    return Optional.of(token);
                } else {
                    return Optional.empty();
                }
            } else {
                FileUtils.writeStringToFile(tokenFile, "TOKEN", (String) null);
                return Optional.empty();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static String readJSON(String key) {
        return (String) config.get(key);
    }

    public static void writeJSON(String key, String setting) {
        if (config.containsKey(key)) {
            config.replace(key, setting);
        } else {
            config.putIfAbsent(key, setting);
        }
        try {
            FileWriter file = new FileWriter(botPath + "/TVproperties.json");
            file.write(config.toJSONString());
            file.flush();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(IChannel channel, String message) {
        try {
            channel.sendMessage(message);
        } catch (Exception ignored) {
        }
    }

    public static IMessage sendBufferedMessage(IChannel channel, String message) {
        RequestBuffer.RequestFuture<IMessage> sentMessage = RequestBuffer.request(() -> {
            try {
                return channel.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        return sentMessage.get();
    }

    public static void deleteMessage(IMessage message) {
        try {
            message.delete();
        } catch (Exception ignored) {
        }
    }

    public static void sendPrivateMessage(IUser user, String message) {
        try {
            user.getClient().getOrCreatePMChannel(user).sendMessage(message);
        } catch (Exception ignored) {
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

}
