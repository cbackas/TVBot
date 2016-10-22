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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class CommandHelp implements Command {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("commands");
    }

    static final String userCommands =
            "------------------------------------------------------------------\n" +
                    "!help                                  //shows a list of commands\n" +
                    "     aliases: !commands\n" +
                    "!stats                                 //shows information about the server \n" +
                    "     aliases: !info, !serverinfo, !server\n" +
                    "!goodnight                             //does that good night thing everyone likes\n" +
                    "!lenny                                 //lenny\n" +
                    "!shrug                                 //shrugs\n" +
                    "!suggest [stuff]                       //pins your suggestion in #suggestion\n" +
                    "     aliases: !idea, !suggestion\n" +
                    "!search [show name]                    //gives info about a show\n" +
                    "     aliases: !lookup, !show";

    static final String trialmodCommands =
            "\n------------------------------------------------------------------\n" +
                    "!addlog [message]                      //adds a message to the log\n" +
                    "     aliases: !log\n" +
                    "!mute @user [reason?]                  //mutes user\n" +
                    "!unmute @user                          //unmutes user";

    static final String modCommands =
            "\n------------------------------------------------------------------\n" +
                    "!addlog [message]                      //adds a message to the log\n" +
                    "     aliases: !log\n" +
                    "!prune [#] @user                         //deletes a number of messages by a user\n" +
                    "     aliases: !purge\n" +
                    "!mute @user [reason?]                  //mutes user\n" +
                    "!unmute @user                          //unmutes user\n" +
                    "!embedmute @user                       //removes users embed perms\n" +
                    "!unembedmute @user                     //restores users embed perms\n" +
                    "!ban @user [reason?]                   //bans user and logs the action";

    static final String devCommands =
            "\n------------------------------------------------------------------\n" +
                    "!addshow [imdbid] [here|channelid]     //adds a new show to the calendar\n" +
                    "!removeshow [imdbid]                   //deletes a show from the calendar\n" +
                    "!addchannel [channel name]             //creates a new channel with the given name\n" +
                    "!showid [here|showname]                //returns possible imdb id for a show\n" +
                    "!roleid [rolename|listall]             //gives the roleid for role\n" +
                    "!setmuteperm                           //gives muted role to all channels\n" +
                    "!aunmute @user                         //unmutes user without log";

    static final String adminCommands =
            "\n------------------------------------------------------------------\n" +
                    "!addshow [imdbid] [here|channelid]     //adds a new show to the calendar\n" +
                    "!removeshow [imdbid]                   //deletes a show from the calendar\n" +
                    "!showid [here|showname]                //returns possible imdb id for a show\n" +
                    "!announce [announcement]               //sends a public announcement\n" +
                    "!addchannel [channel name]             //creates a new channel with the given name\n" +
                    "     aliases: !newchannel\n" +
                    "!prune [#] @user                       //deletes a number of messages by a user\n" +
                    "     exclude user to purge all user's messages\n" +
                    "     aliases: !purge\n" +
                    "!resetxp @user                         //resets a users message count\n" +
                    "!embedmute @user                       //removes users embed perms\n" +
                    "!unembedmute @user                     //restores users embed perms\n" +
                    "!amute @user                           //mutes user without log\n" +
                    "!aunmute @user                         //unmutes user without log\n" +
                    "!addlog [message]                      //adds a message to the log\n" +
                    "     aliases: !log\n" +
                    "!mute @user [reason?]                  //mutes user\n" +
                    "!unmute @user                          //unmutes user\n" +
                    "!kick @user [reason]                  //kicks user and logs the action\n" +
                    "!ban @user [reason]                   //bans user and logs the action";

    String movieCommands =
            "\n------------------------------------------------------------------\n" +
                    "!movienight set [pollID] [date]        //posts a link to a the google poll\n" +
                    "!movienight announce [movie]           //deletes poll and announces movie\n" +
                    "!movienight start [rabbitID]           //announces movienight start and links to room";

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        guild = client.getGuildByID("192441520178200577");
        List<IRole> roles = message.getAuthor().getRolesForGuild(guild);
        String finalHelp = userCommands;
        if (roles.contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) { //Staff Check
            finalHelp += "\n!rule [number]                         //posts the rule requested in chat";
        }
        if (roles.contains(guild.getRoleByID(TVBot.MOVIENIGHT_ROLE_ID))) { //Movienight Check
            finalHelp += movieCommands;
        }
        if (roles.contains(guild.getRoleByID("228231762113855489"))) { //Trialmod Check
            finalHelp += trialmodCommands;
        }
        if (roles.contains(guild.getRoleByID(TVBot.MOD_ROLE_ID))) { //Mod Check
            finalHelp += modCommands;
        }
        if (roles.contains(guild.getRoleByID(TVBot.DEV_ROLE_ID))) { //Dev Check
            finalHelp += devCommands;
        }
        if (roles.contains(guild.getRoleByID(TVBot.ADMIN_ROLE_ID))) { //Admin Check
            finalHelp += adminCommands;
        }
        try {
            new MessageBuilder(client).withChannel(message.getAuthor().getOrCreatePMChannel()).appendQuote("TVBot's Commands:").appendCode("xl", finalHelp).appendQuote("Staff commands excluded for regular users").send();
        } catch (Exception e) {
        }
        Util.deleteMessage(message);

    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
