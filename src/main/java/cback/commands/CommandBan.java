package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandBan extends Command {

    private TVBot bot;

    public CommandBan() {
        this.bot = TVBot.getInstance();
        this.name = "ban";
        this.help = "Bans a user from the server and logs the reason";
        this.arguments = "ban @user [reason]";
        this.requiredRole = TVRoles.STAFF.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        PermissionUtil.checkPermission(commandEvent.getTextChannel(), commandEvent.getMember(), Permission.BAN_MEMBERS);
        Pattern pattern = Pattern.compile("^!ban <@!?(\\d+)> ?(.+)?");
        Matcher matcher = pattern.matcher(commandEvent.getMessage().getContentRaw());
        GuildController gC = new GuildController(commandEvent.getGuild());
        if(matcher.find()) {
            String userInput = matcher.group(1);
            String reason = matcher.group(2);
            Member user = commandEvent.getGuild().getMemberById(Long.parseLong(userInput));
            if(reason != null && user != null) {
                if(user.getEffectiveName().equals(commandEvent.getAuthor().getId())) {
                    Util.sendMessage(commandEvent.getTextChannel(), "You're gonna have to try harder than that.");
                } else {
                    try {
                        gC.ban(user.getUser(), 1, reason + " Appeal at https://www.reddit.com/r/LoungeBan/").queue();
                        Util.sendLog(commandEvent.getMessage(), "Banned " + user.getEffectiveName() + "\n**Reason:** " + reason, Color.RED);
                        Util.simpleEmbed(commandEvent.getTextChannel(), user.getEffectiveName() + " has been banned. Check " + commandEvent.getGuild().getTextChannelById(Channels.SERVERLOG_CH_ID.getId()).getAsMention() + " for more info.");
                    } catch(Exception ex) {
                        Util.simpleEmbed(commandEvent.getTextChannel(), "Error running " + this.getName() + " - error recorded");
                        Util.reportHome(commandEvent.getMessage(), ex);
                    }
                }
            } else {
                Util.sendPrivateMessage(commandEvent.getAuthor(), "**Error Banning**: Reason required");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}
