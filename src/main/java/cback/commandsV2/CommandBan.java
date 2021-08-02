package cback.commandsV2;

import cback.Channels;
import cback.TVRoles;
import cback.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.awt.*;
import java.time.Instant;


public class CommandBan extends Command {
    public CommandBan() {
        super();

        // build command data to be sent to discord for slash commands
        this.commandData = new CommandData("ban", "Bans a user from the server and logs it")
                .addOption(OptionType.USER, "user", "User to ban", true)
                .addOption(OptionType.STRING, "reason", "Reason for the ban", true)
//                .addOption(OptionType.)
                .setDefaultEnabled(false);

        this.commandPrivileges.add(new CommandPrivilege(CommandPrivilege.Type.ROLE, true, TVRoles.MOD.id));
    }

    @Override
    public void execute(SlashCommandEvent event) {
        MessageEmbed loadingEmbed = new EmbedBuilder()
                .setColor(Util.getBotColor())
                .setDescription("Banning ...")
                .build();

        event.replyEmbeds(loadingEmbed)
                .setEphemeral(true)
                .flatMap(v -> {
                    var userToBan = event.getOption("user").getAsMember();
                    var reason = event.getOption("reason").getAsString();

                    boolean argsNull = reason == null || userToBan == null;
                    boolean selfBan = userToBan.getId().equals(event.getUser().getId());
                    boolean banFailed = !banUser(event.getGuild(), userToBan, reason, 1);

                    EmbedBuilder responseEmbedBuilder = new EmbedBuilder();

                    if (argsNull) {
//                      Util.sendPrivateMessage(event.getAuthor(), "**Error Banning**: Reason required");
                        responseEmbedBuilder.setTitle("**Error Banning**")
                                .setDescription("User or reason is null")
                                .setColor(Color.red);
                        return event.getHook().editOriginalEmbeds(responseEmbedBuilder.build());
                    } else if (selfBan) {
//                      Util.sendMessage(event.getTextChannel(), "You're gonna have to try harder than that.");
                        responseEmbedBuilder.setTitle("**Error Banning**")
                                .setDescription("Oops! You can't ban yourself.")
                                .setColor(Color.red);
                        return event.getHook().editOriginalEmbeds(responseEmbedBuilder.build());
                    } else if (banFailed) {
                        // if banUser errors
//                      Util.simpleEmbed(event.getTextChannel(), "Error running " + this.getName() + " - error recorded");
                        return event.getHook().editOriginalEmbeds(responseEmbedBuilder.build());
                    } else {
                        // delete original ephemeral response
                        event.getHook().deleteOriginal().queue();

                        // put log in server's bot log channel
//                      Util.sendLog(event.getMessage(), "Banned " + userToBan.getEffectiveName() + "\n**Reason:** " + reason, Color.RED);
                        EmbedBuilder logEmbedBuilder = new EmbedBuilder().setTitle("Banned " + userToBan.getEffectiveName())
                                .addField("Reason", reason, false)
                                .setFooter("Action by @" + event.getMember().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .setColor(Color.RED);

                        Channels.BOTLOG_CH_ID.getChannel()
                                .sendMessageEmbeds(logEmbedBuilder.build())
                                .queue();


                        // respond publicly to user who did ban command
//                      Util.simpleEmbed(event.getTextChannel(), userToBan.getEffectiveName() + " has been banned. Check " + event.getGuild().getTextChannelById(Channels.SERVERLOG_CH_ID.getId()).getAsMention() + " for more info.");
                        responseEmbedBuilder.setTitle("Banned " + userToBan.getEffectiveName())
                                .setDescription("Check " + event.getGuild().getTextChannelById(Channels.SERVERLOG_CH_ID.getId()).getAsMention() + " for more info.")
                                .setColor(Color.RED);


                        return event.getHook()
                                .sendMessageEmbeds(responseEmbedBuilder.build())
                                .setEphemeral(false);

                    }
                })
                .queue();
    }

    private boolean banUser(Guild guild, Member member, String reason, int delDays) {
        try {
//          guild.ban(member.getUser(), delDays, reason + " Appeal at https://www.reddit.com/r/LoungeBan/").submit();
            return true;
        } catch (Exception e) {
            Util.reportHome(e);
            return false;
        }
    }
}
