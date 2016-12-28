package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

public class CommandStaffSummon implements Command {
    @Override
    public String getName() {
        return "staff";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        //Lounge Command Only
        if (guild.getID().equals("192441520178200577")) {
            if (!bot.getConfigManager().getConfigArray("cantsummon").contains(message.getAuthor().getID())) {
                IChannel staffHQ = client.getChannelByID("226433456060497920");
                List<IUser> mods = Util.getUsersByRole(TVRoles.MOD.id);

                StringBuilder modMentions = new StringBuilder();
                for (IUser u : mods) {
                    modMentions.append(" ").append(u.mention());
                }

                Util.sendMessage(message.getChannel(), "Staff have been notified and will come shortly.");
                Util.sendMessage(staffHQ, message.getAuthor().mention() + " has requested staff in " + message.getChannel().mention() + "\n" + modMentions.toString());

                Util.botLog(message);
                Util.deleteMessage(message);
            }
        }
    }
}
