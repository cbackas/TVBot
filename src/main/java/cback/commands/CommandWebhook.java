package cback.commands;

import cback.TVBot;
import cback.Util;
import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandWebhook implements Command {
    @Override
    public String getName() {
        return "webhook";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public List<String> getPermissions() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        if (message.getAuthor().getID().equals("73416411443113984")) {
            Pattern pattern = Pattern.compile("^!trigger <@!?(\\d+)> \"(.+)\"");
            Matcher matcher = pattern.matcher(message.getContent());
            if (matcher.find()) {
                IUser user = guild.getUserByID(matcher.group(1));
                String words = matcher.group(2);

                try {
                    new Slack("https://ptb.discordapp.com/api/webhooks/251482202527760385/OkWClC55zDGDkfNaXgHquq0n5vXNNXDCi6P0bUDLqKdrD1Z4WnSueMO-zmvVG6LpCdHb/slack")
                            .icon(user.getAvatarURL())
                            .displayName(user.getDisplayName(guild))
                            .push(new SlackMessage(words));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Util.deleteMessage(message);
        }
    }
}
