package cback.commands;

import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.xp.UserXP;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

public class CommandXPReset implements Command{

    @Override
    public String getName() {
        return "resetxp";
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
        List<IRole> userRoles = message.getAuthor().getRolesForGuild(guild);
        if (userRoles.contains(guild.getRoleByID(TVRoles.ADMIN.id))) {
            if(args.length >= 1){
                IUser mentioned = Util.getUserFromMentionArg(args[0]);
                if(mentioned != null){
                    UserXP xp = bot.getDatabaseManager().getXP().getUserXP(mentioned.getID());
                    if(xp != null){
                        xp.setMessageCount(0);
                        bot.getDatabaseManager().getXP().updateUserXP(xp);
                        Util.sendMessage(message.getChannel(), "Reset xp for " + mentioned.getDisplayName(guild));
                    }else{
                        Util.sendMessage(message.getChannel(), "No xp data found for " + mentioned.getDisplayName(guild));
                    }
                }else{
                    Util.sendMessage(message.getChannel(), "Invalid user \"" + args[0] + "\".");
                }
            }else{
                Util.sendMessage(message.getChannel(), "Usage: !resetxp @user");
            }

            Util.botLog(message);
            Util.deleteMessage(message);
        }
    }

}
