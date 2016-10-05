package cback.commands;

import cback.TVBot;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.MessageBuilder;

import java.util.EnumSet;

public class CommandHelp implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        try {
            DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
            new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL",
                    "!help                              //shows a list of commands\n" +
                            "!addshow [imdbid] [here|channelid] //adds a new show to the calendar\n" +
                            "!removeshow [imdbid]               //deletes a show from the calendar\n" +
                            "!addlog [message]                  //adds a message to the log\n" +
                            "!mute @user                        //mutes user\n" +
                            "!unmute @user                      //unmutes user\n" +
                            "!kick @user [reason]               //kicks user and logs the action\n" +
                            "!ban @user [reason]                //bans user and logs the action\n" +
                            "!rule [number]                     //posts the rule requested in chat"
            ).appendQuote("Mod+ commands included - regular users can not see staff commands").send();
        } catch (Exception e) {
            try {
                new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL",
                        "!help                          //shows a list of commands\n" +
                                "!goodnight                     //does that good night thing everyone likes\n" +
                                "!lenny                         //lenny"
                ).appendQuote("This bot will get some cool commands down the road").send();
            } catch (Exception f) {
            }
        }
    }
}
