package cback.commands;

import cback.TVBot;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.MessageBuilder;

import java.util.EnumSet;
import java.util.List;

public class CommandHelp implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String userCommands =
                "!help                                  //shows a list of commands\n" +
                "!goodnight                             //does that good night thing everyone likes\n" +
                "!lenny                                 //lenny\n" +
                "!shrug                                 //shrugs";

        String modCommands =
                "!addshow [imdbid] [here|channelid]     //adds a new show to the calendar\n" +
                "!removeshow [imdbid]                   //deletes a show from the calendar\n" +
                "!addlog [message]                      //adds a message to the log\n" +
                "!mute @user                            //mutes user\n" +
                "!unmute @user                          //unmutes user\n" +
                "!kick @user [reason]                   //kicks user and logs the action\n" +
                "!ban @user [reason]                    //bans user and logs the action\n" +
                "!rule [number]                         //posts the rule requested in chat";

        String adminCommands =
                "!amute @user                           //mutes user without log\n" +
                "!aunmute @user                         //unmutes user without log";

        String movieCommands =
                "!movienight set [pollID] [date]        //posts a link to a the google poll\n" +
                        "!movienight announce [movie]           //deletes poll and announces movie\n" +
                        "!movienight start [rabbitID]           //announces movienight start and links to room";

        List<IRole> roles = message.getAuthor().getRolesForGuild(guild);
        if (roles.contains(guild.getRoleByID("226443478664609792"))) { //Movienight check
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL", userCommands).appendCode("XL", modCommands).appendCode("XL", movieCommands).appendQuote("Mod+ commands included - regular users can not see staff commands").send();
            } catch (Exception e) {
                try {
                    new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL", userCommands).appendCode("XL", movieCommands).appendQuote("This bot will get some cool commands down the road").send();
                } catch (Exception f) {
                }
            }
        } else if (roles.contains(guild.getRoleByID("192441946210435072"))) { //Admin check
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL", userCommands).appendCode("XL", modCommands).appendCode("XL", adminCommands).appendCode("XL", movieCommands).appendQuote("Mod+ commands included - regular users can not see staff commands").send();
            } catch (Exception e) {
            }
        } else { //All users mod and below
            try {
                DiscordUtils.checkPermissions(message.getChannel().getModifiedPermissions(message.getAuthor()), EnumSet.of(Permissions.BAN));
                new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL", userCommands).appendCode("XL", modCommands).appendQuote("Mod+ commands included - regular users can not see staff commands").send();
            } catch (Exception e) {
                try {
                    new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("XL", userCommands).appendQuote("This bot will get some cool commands down the road").send();
                } catch (Exception f) {
                }
            }
        }
        Util.deleteMessage(message);
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
