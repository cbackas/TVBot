package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandChannelRemove implements Command {
    @Override
    public String getName() {
        return "deletechannel";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("removechannel");
    }

    public static List<String> permitted = Arrays.asList(TVRoles.ADMIN.id, TVRoles.NETWORKMOD.id, TVRoles.HEADMOD.id);

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        List<String> userRoles = message.getAuthor().getRolesForGuild(guild).stream().map(role ->role.getID()).collect(Collectors.toList());
        if (!Collections.disjoint(userRoles, permitted)) {

            List<IChannel> mentionsC = message.getChannelMentions();
            if (!mentionsC.isEmpty()) {
                for (IChannel c : mentionsC) {
                    try {

                        Util.sendLog(message, "Deleted " + c.getName() + " channel.");

                        c.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (args[0].equalsIgnoreCase("here")) {
                try {

                    IChannel here = message.getChannel();

                    Util.sendLog(message, "Deleted " + here.getName() + " channel.");
                    here.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Util.sendMessage(message.getChannel(), "**ERROR**: Couldn't find channel to delete.");
            }

            Util.botLog(message);
            Util.deleteMessage(message);
        }
    }

}
