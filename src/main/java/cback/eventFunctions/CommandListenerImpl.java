package cback.eventFunctions;

import cback.Channels;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.nibor.autolink.LinkExtractor;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommandListenerImpl implements CommandListener {

    private TVBot bot = TVBot.getInstance();

    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?s)^" + TVBot.COMMAND_PREFIX + "([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);
    private static LinkExtractor linkExtractor = LinkExtractor.builder().build();

    @Override
    public void onCommand(CommandEvent event, Command command) {
        Util.getLogger().info(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + " issued command in " + event.getChannel().getName() + " - " + event.getMessage().getContentRaw());
    }

    @Override
    public void onNonCommandMessage(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return; //ignore bot messages
        boolean isPrivate = event.getMessage().isFromType(ChannelType.PRIVATE);

        var message = event.getMessage();
        String text = event.getMessage().getContentRaw();

        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.matches()) {
            //check for custom command
            String baseCommand = matcher.group(1).toLowerCase();
            if (bot.getCustomCommandManager().getCommandValue(baseCommand) != null) {
                //send custom response
                String response = bot.getCustomCommandManager().getCommandValue(baseCommand);
                Util.sendMessage(message.getTextChannel(), "``" + message.getAuthor().getName() + "``\n" + response);
                Util.deleteMessage(message);
            }

        } else if (isPrivate) {
            //Forwards the random stuff people PM to the bot - to me
            MessageEmbed embed = Util.buildBotPMEmbed(message, 1);
            Util.sendEmbed(Channels.BOTPM_CH_ID.getChannel(), embed);
        } else {
            //below here are just regular chat messages

            //do the censoring
            censorMessages(message);
            censorLinks(message);
            censorMentions(message);

            //Increment message count if message was not a command
            bot.getDatabaseManager().getXP().addXP(message.getAuthor().getId(), 1);

            //Messages containing my name go to botpms now too cuz im watching//
            if (message.getContentRaw().toLowerCase().contains("cback")) {
                MessageEmbed embed = Util.buildBotPMEmbed(message, 2);
                Util.sendEmbed(Channels.BOTPM_CH_ID.getChannel(), embed);
            }
        }
    }

    /**
     * Checks for excessive @ mentions
     */
    public void censorMentions(Message message) {

        boolean staffMember = message.getAuthor().getJDA().getRoles().contains(message.getGuild().getRoleById(TVRoles.STAFF.id));
        if (!staffMember && bot.getToggleState("limitmentions")) {
            int mentionCount = message.getMentions(Message.MentionType.USER, Message.MentionType.EVERYONE, Message.MentionType.HERE).size();
            if (mentionCount > 10) {
                try {
                    message.getGuild().getController().ban(message.getAuthor(), 0, "Mentioned more than 10 users in a message. Appeal at https://www.reddit.com/r/LoungeBan/").queue();
                    Util.simpleEmbed(message.getTextChannel(), message.getAuthor().getName() + " was just banned for mentioning more than 10 users.");
                    Util.sendLog(message, "Banned " + message.getAuthor().getName() + "\n**Reason:** Doing too many @ mentions", Color.red);
                } catch (Exception e) {
                    Util.reportHome(e);
                }
            } else if (mentionCount > 5) {
                Util.deleteMessage(message);
            }
        }

    }

    /**
     * Checks for dirty words :o
     */
    public void censorMessages(Message message) {
        if (bot.getToggleState("censorwords")) {
            User author = message.getAuthor();

            boolean homeGuild = message.getGuild().getIdLong() == TVBot.HOMESERVER_GLD_ID;
            //TODO which categories are these
            boolean staffChannel = message.getCategory().getIdLong() == 355901035597922304L || message.getCategory().getIdLong() == 355910636464504832L;
            boolean staffMember = author.getJDA().getRoles().contains(message.getGuild().getRoleById(TVRoles.STAFF.id));

            if (homeGuild && !staffChannel && !staffMember) {
                var bannedWords = bot.getConfigManager().getConfigArray("bannedWords");
                String content = message.getContentDisplay().toLowerCase();

                String word = "";
                boolean tripped = false;
                for (String w : bannedWords) {
                    if (content.matches("\\n?.*\\b\\n?" + w + "\\n?\\b.*\\n?.*") || content.matches("\\n?.*\\b\\n?" + w + "s\\n?\\b.*\\n?.*")) {
                        tripped = true;
                        word = w;
                        break;
                    }
                }
                if (tripped) {

                    EmbedBuilder bld = new EmbedBuilder();
                    bld.setAuthor(Util.getTag(author), author.getEffectiveAvatarUrl()).setDescription(message.getContentDisplay()).setTimestamp(Instant.now()).setFooter("Auto-deleted from #" + message.getChannel().getName(), null);

                    Util.sendEmbed(message.getGuild().getTextChannelById(Channels.MESSAGELOG_CH_ID.getId()), bld.setColor(Util.getBotColor()).build());

                    StringBuilder sBld =
                            new StringBuilder().append("Your message has been automatically removed for containing a banned word. If this is an error, message a staff member.");
                    if (!word.isEmpty()) {
                        sBld.append("\n\n").append(word);
                    }

                    Util.sendPrivateEmbed(author, sBld.toString());
                    Util.deleteMessage(message);
                }
            }
        }
    }

    /**
     * Censor links
     */
    public void censorLinks(Message message) {
        if (bot.getToggleState("censorlinks")) {
            User author = message.getAuthor();

            boolean homeGuild = message.getGuild().getIdLong() == TVBot.HOMESERVER_GLD_ID;
            //TODO what are these categories?
            boolean staffChannel = message.getCategory().getIdLong() == 355901035597922304L || message.getCategory().getIdLong() == 355910636464504832L;
            boolean staffMember = author.getJDA().getRoles().contains(message.getGuild().getRoleById(TVRoles.STAFF.id));


            //trusted if you have a role of trusted or higher
            boolean trusted = false;
            var userRoles = message.getMember().getRoles();
            int trustedPos = message.getGuild().getRoleById(TVRoles.TRUSTED.id).getPosition();
            for (Role r : userRoles) {
                if (r.getPosition() >= trustedPos) {
                    trusted = true;
                    break;
                }
            }

            if (homeGuild && !staffChannel && !staffMember && !trusted) {
                String content = message.getContentDisplay().toLowerCase();

                var linksFound = new ArrayList<>();
                var links = linkExtractor.extractLinks(content);
                links.forEach(l -> linksFound.add(message.getContentRaw().substring(l.getBeginIndex(), l.getEndIndex())));

                if (linksFound.size() >= 1) {

                    String collectedLinks = StringUtils.join(linksFound, "\n");

                    EmbedBuilder bld = new EmbedBuilder();
                    bld.setAuthor(Util.getTag(author), author.getEffectiveAvatarUrl()).setDescription(message.getContentDisplay()).setTimestamp(Instant.now()).setFooter("Auto-deleted from #" + message.getChannel().getName(), null);

                    Util.sendEmbed(message.getGuild().getTextChannelById(Channels.MESSAGELOG_CH_ID.getId()), bld.setColor(Util.getBotColor()).build());
                    Util.sendPrivateEmbed(author, "Your message has been automatically removed for containing a link. If this is an error, message a staff member.\n\n" + collectedLinks);
                    Util.deleteMessage(message);
                }
            }
        }
    }
}