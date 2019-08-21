package cback;

//import cback.commands.*;

import cback.database.DatabaseManager;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVBot {

    private static TVBot instance;
    private JDA jda;

    private DatabaseManager databaseManager;
    private TraktManager traktManager;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private ToggleManager toggleManager;
    private Scheduler scheduler;

    public List<Command> registeredCommands = new ArrayList<>();
    private List<String> commandPrefixes = Arrays.asList("!", "t!", "!g", "--", ".", "?");

    private long startTime;

    public static final String COMMAND_PREFIX = "!";
    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?s)^" + COMMAND_PREFIX + "([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);

    public static final long CBACK_USR_ID = 73416411443113984l;
    public static final long HOMESERVER_GLD_ID = 192441520178200577l;

    public static final long UNSORTED_CAT_ID = 358043583355289600L;
    public static final long STAFF_CAT_ID = 355901035597922304L;
    public static final long INFO_CAT_ID = 355910636464504832L;
    public static final long DISCUSSION_CAT_ID = 355910667812995084L;
    public static final long FUN_CAT_ID = 358679449451102210L;
    public static final long CLOSED_CAT_ID = 355904962200469504L;
    public static final long AF_CAT_ID = 358038418208587785L;
    public static final long GL_CAT_ID = 358038474894606346L;
    public static final long MR_CAT_ID = 358038505244327937L;
    public static final long SZ_CAT_ID = 358038532780195840L;

    public static void main(String[] args) throws LoginException, InterruptedException {
        new TVBot();
    }

    public TVBot() throws LoginException, InterruptedException {
        instance = this;

        //instantiate config manager first as connect() relies on tokens
        configManager = new ConfigManager(this);
        commandManager = new CommandManager(this);
        toggleManager = new ToggleManager(this);

        connect();

        databaseManager = new DatabaseManager(this);
        traktManager = new TraktManager(this);
        scheduler = new Scheduler(this);
    }

    private void connect() throws LoginException, InterruptedException {
        //don't load external modules and don't attempt to create modules folder
        //Configuration.LOAD_EXTERNAL_MODULES = false;

        Optional<String> token = configManager.getTokenValue("botToken");
        if (token.isEmpty()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your bot's token in the config.");
            System.out.println("Exiting......");
            System.out.println("-------------------------------------");
            System.exit(0);
            return;
        }

        var commandClientBuilder = new CommandClientBuilder();
        commandClientBuilder.setOwnerId(String.valueOf(CBACK_USR_ID));
        commandClientBuilder.setPrefix(COMMAND_PREFIX);
        commandClientBuilder.setGame(Game.watching("all of your messages. Type " + COMMAND_PREFIX + "help"));

        new Reflections("cback.commands").getSubTypesOf(Command.class).forEach(commandImpl -> {
            try {
                Command command = commandImpl.getDeclaredConstructor().newInstance();
                Optional<Command> existingCommand = registeredCommands.stream()
                        .filter(cmd -> cmd.getName().equalsIgnoreCase(command.getName()))
                        .findAny();
                if (!existingCommand.isPresent()) {
                    registeredCommands.add(command);
                    System.out.println("Registered command: " + command.getName());
                } else {
                    System.out.println("Attempted to register two commands with the same name: " + existingCommand.get().getName());
                    System.out.println("Existing: " + existingCommand.get().getClass().getName());
                    System.out.println("Attempted: " + commandImpl.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        registeredCommands.forEach(c -> commandClientBuilder.addCommand(c));

        JDABuilder builder = new JDABuilder(AccountType.BOT).setToken(token.get()).addEventListener(commandClientBuilder.build());
        this.jda = builder.build();

        startTime = System.currentTimeMillis();
    }

    /*
     * Message Central Choo Choo
     */
    public void onMessageEvent(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return; //ignore bot messages
        Message message = event.getMessage();
        boolean isPrivate = message.isFromType(ChannelType.PRIVATE);
        Guild guild = null;
        if (!isPrivate) guild = message.getGuild();
        String text = message.getContentRaw();

        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.matches()) {
            String baseCommand = matcher.group(1).toLowerCase();
            Optional<Command> command = registeredCommands.stream()
                    .filter(com -> com.getName().equalsIgnoreCase(baseCommand) || (com.getAliases() != null && List.of(com.getAliases()).contains(baseCommand)))
                    .findAny();

            if (!command.isPresent() && commandManager.getCommandValue(baseCommand) != null) {
                String response = commandManager.getCommandValue(baseCommand);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("``" + message.getAuthor().getName() + "``\n").append(response);

                Util.sendMessage(message.getTextChannel(), stringBuilder.toString());

                Util.deleteMessage(message);
            }
        } else if (isPrivate) {
            //Forwards the random stuff people PM to the bot - to me
            MessageEmbed embed = Util.buildBotPMEmbed(message, 1);
            Util.sendEmbed(Channels.BOTPM_CH_ID.getChannel(), embed);
        } else {
            //below here are just regular chat messages
            censorMessages(message);
            censorLinks(message);

            //Deletes messages/bans users for using too many @mentions
            boolean staffMember = message.getAuthor().getJDA().getRoles().contains(message.getGuild().getRoleById(TVRoles.STAFF.id));
            if (!staffMember && toggleState("limitmentions")) {
                int mentionCount = message.getMentions(Message.MentionType.USER, Message.MentionType.EVERYONE, Message.MentionType.HERE).size();
                if (mentionCount > 10) {
                    try {
                        guild.getController().ban(message.getAuthor(), 0, "Mentioned more than 10 users in a message. Appeal at https://www.reddit.com/r/LoungeBan/");
                        Util.simpleEmbed(message.getTextChannel(), message.getAuthor().getName() + " was just banned for mentioning more than 10 users.");
                        Util.sendLog(message, "Banned " + message.getAuthor().getName() + "\n**Reason:** Doing too many @ mentions", Color.red);
                    } catch (Exception e) {
                        Util.reportHome(e);
                    }
                } else if (mentionCount > 5) {
                    Util.deleteMessage(message);
                }
            }

            //Increment message count if message was not a command
            databaseManager.getXP().addXP(message.getAuthor().getId(), 1);

            //Messages containing my name go to botpms now too cuz im watching//
            if (message.getContentRaw().toLowerCase().contains("cback")) {
                MessageEmbed embed = Util.buildBotPMEmbed(message, 2);
                Util.sendEmbed(Channels.BOTPM_CH_ID.getChannel(), embed);
            }
        }
    }

    /**
     * Checks for dirty words :o
     */
    public void censorMessages(Message message) {
        if (toggleState("censorwords")) {
            User author = message.getAuthor();

            boolean homeGuild = message.getGuild().getIdLong() == TVBot.HOMESERVER_GLD_ID;
            boolean staffChannel =
                    message.getCategory().getIdLong() == 355901035597922304L || message.getCategory().getIdLong() == 355910636464504832L;
            boolean staffMember = author.getJDA().getRoles().contains(message.getGuild().getRoleById(TVRoles.STAFF.id));

            if (homeGuild && !staffChannel && !staffMember) {
                List<String> bannedWords = getConfigManager().getConfigArray("bannedWords");
                String content = message.getContentDisplay().toLowerCase();

                String word = "";
                Boolean tripped = false;
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
        if (toggleState("censorlinks")) {
            User author = message.getAuthor();

            boolean homeGuild = message.getGuild().getIdLong() == TVBot.HOMESERVER_GLD_ID;
            boolean staffChannel =
                    message.getCategory().getIdLong() == 355901035597922304L || message.getCategory().getIdLong() == 355910636464504832L;
            boolean staffMember = author.getJDA().getRoles().contains(message.getGuild().getRoleById(TVRoles.STAFF.id));


            boolean trusted = false;
            List<Role> userRoles = author.getJDA().getRoles();
            int tPos = jda.getRoleById(TVRoles.TRUSTED.id).getPosition();
            for (Role r : userRoles) {
                int rPos = r.getPosition();
                if (rPos >= tPos) {
                    trusted = true;
                    break;
                }
            }

            if (homeGuild && !staffChannel && !staffMember && !trusted) {
                String content = message.getContentDisplay().toLowerCase();
                List<String> linksFound = new ArrayList<>();

                LinkExtractor linkExtractor = LinkExtractor.builder().build();
                Iterable<LinkSpan> links = linkExtractor.extractLinks(content);
                if (links.iterator().hasNext()) {
                    for (LinkSpan l : links) {
                        String f = message.getContentRaw().substring(l.getBeginIndex(), l.getEndIndex());
                        linksFound.add(f);
                    }
                }

                if (linksFound.size() >= 1) {
                    String collectedLinks = "";
                    for (String s : linksFound) {
                        collectedLinks += s + " ";
                    }

                    EmbedBuilder bld = new EmbedBuilder();
                    bld.setAuthor(Util.getTag(author), author.getEffectiveAvatarUrl()).setDescription(message.getContentDisplay()).setTimestamp(Instant.now()).setFooter("Auto-deleted from #" + message.getChannel().getName(), null);

                    Util.sendEmbed(message.getGuild().getTextChannelById(Channels.MESSAGELOG_CH_ID.getId()), bld.setColor(Util.getBotColor()).build());
                    Util.sendPrivateEmbed(author, "Your message has been automatically removed for containing a link. If this is an error, message a staff member.\n\n" + collectedLinks);
                    Util.deleteMessage(message);
                }
            }
        }
    }

    public static TVBot getInstance() {
        return instance;
    }

    public List<Command> getRegisteredCommands() {
        return registeredCommands;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TraktManager getTraktManager() {
        return traktManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ToggleManager getToggleManager() {
        return toggleManager;
    }

    public JDA getClient() {
        return jda;
    }

    public Guild getHomeGuild() {
        return jda.getGuildById(Long.parseLong(configManager.getConfigValue("HOMESERVER_ID")));
    }

    public Guild getGuild() {
        return jda.getGuildById("247394948331077632"); // todo CHANGE THIS
    }

    public String getUptime() {
        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = (totalSeconds / 3600);
        return (hours < 10 ? "0" + hours : hours) + "h " + (minutes < 10 ? "0" + minutes : minutes) + "m " + (seconds < 10 ? "0" + seconds : seconds) + "s";
    }

    /**
     * Setting toggles
     */
    public boolean toggleSetting(String toggleKey) {
        toggleManager.toggleToggleValue(toggleKey);
        return toggleManager.getToggleValue(toggleKey);
    }

    /**
     * Get toggle bool
     */
    public boolean toggleState(String toggleKey) {
        return toggleManager.getToggleValue(toggleKey);
    }
}
