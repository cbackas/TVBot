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

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String userCommands =
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

        String trialmodCommands =
                "\n------------------------------------------------------------------\n" +
                        "!addlog [message]                      //adds a message to the log\n" +
                        "!mute @user [reason?]                  //mutes user\n" +
                        "!unmute @user                          //unmutes user";

        String modCommands =
                "\n------------------------------------------------------------------\n" +
                "!addlog [message]                      //adds a message to the log\n" +
                        "!mute @user [reason?]                  //mutes user\n" +
                        "!unmute @user                          //unmutes user\n" +
                        "!kick @user [reason?]                  //kicks user and logs the action\n" +
                        "!ban @user [reason?]                   //bans user and logs the action";

        String devCommands =
                "\n------------------------------------------------------------------\n" +
                        "!roleid [rolename|listall]             //gives the roleid for role\n" +
                        "!setmuteperm                           //gives muted role to all channels\n" +
                        "!aunmute @user                         //unmutes user without log";

        String zockCommands =
                "\n------------------------------------------------------------------\n" +
                        "!addshow [imdbid] [here|channelid]     //adds a new show to the calendar\n" +
                        "!removeshow [imdbid]                   //deletes a show from the calendar\n" +
                        "!amute @user                           //mutes user without log\n" +
                        "!announce [announcement]               //sends a public announcement\n" +
                        "!aunmute @user                         //unmutes user without log";

        String adminCommands =
                "\n------------------------------------------------------------------\n" +
                "!addshow [imdbid] [here|channelid]     //adds a new show to the calendar\n" +
                        "!removeshow [imdbid]                   //deletes a show from the calendar\n" +
                        "!announce [announcement]               //sends a public announcement\n" +
                        "!amute @user                           //mutes user without log\n" +
                        "!aunmute @user                         //unmutes user without log\n" +
                        "!addlog [message]                      //adds a message to the log\n" +
                        "!mute @user [reason?]                  //mutes user\n" +
                        "!unmute @user                          //unmutes user\n" +
                        "!kick @user [reason?]                  //kicks user and logs the action\n" +
                        "!ban @user [reason?]                   //bans user and logs the action";

        String movieCommands =
                "\n------------------------------------------------------------------\n" +
                "!movienight set [pollID] [date]        //posts a link to a the google poll\n" +
                        "!movienight announce [movie]           //deletes poll and announces movie\n" +
                        "!movienight start [rabbitID]           //announces movienight start and links to room";

        guild = client.getGuildByID("192441520178200577");
        List<IRole> roles = message.getAuthor().getRolesForGuild(guild);
        String finalHelp = userCommands;
        if (roles.contains(guild.getRoleByID(TVBot.STAFF_ROLE_ID))) { //Staff Check
            finalHelp = userCommands + "\n!rule [number]                         //posts the rule requested in chat";
        } if (roles.contains(guild.getRoleByID("226443478664609792"))) { //Movienight Check
            finalHelp = finalHelp + movieCommands;
        } if (roles.contains(guild.getRoleByID("228231762113855489"))) {
            finalHelp = finalHelp + trialmodCommands;
        } if (roles.contains(guild.getRoleByID("192442068981776384"))) { //Mod Check
            finalHelp = finalHelp + modCommands;
        } if (roles.contains(guild.getRoleByID("236988571330805760"))) { //Dev Check
            finalHelp = finalHelp + devCommands;
        } if (message.getAuthor().getID().equals("148279556619370496")) { //Zock Check
            finalHelp = finalHelp + zockCommands;
        } if (roles.contains(guild.getRoleByID("192441946210435072")) && !message.getAuthor().getID().equals("148279556619370496")) { //Admin Check
            finalHelp = finalHelp + adminCommands;
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
