package cback;

//import cback.commands.*;
import cback.database.DatabaseManager;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import net.dv8tion.jda.client.JDAClient;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;

import javax.security.auth.login.LoginException;

        import java.util.ArrayList;
        import java.util.List;
import java.util.Optional;
        import java.util.regex.Pattern;

public class TVBot {

    private static TVBot instance;
    private static JDAClient client;
    private static JDA jda;

    private DatabaseManager databaseManager;
    //private TraktManager traktManager;
    private static ConfigManager configManager;
    private CommandManager commandManager;
    private ToggleManager toggleManager;
    //private Scheduler scheduler;

    public static ArrayList<Long> messageCache = new ArrayList<>();

    //public static List<Command> registeredCommands = new ArrayList<>();
    static public String prefix = "!";
    public List<String> prefixes = new ArrayList<>();
    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?s)^!([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);

    public static final long CBACK_USR_ID = 73416411443113984L;
    public static final long HOMESERVER_GLD_ID = 192441520178200577L;

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

    /*public static final long ANNOUNCEMENT_CH_ID = 345774506373021716L;
    public static final long NEWEPISODE_CH_ID = 263184398894104577L;
    public static final long GENERAL_CH_ID = 192441520178200577L;
    public static final long SUGGEST_CH_ID = 192444470942236672L;
    public static final long MESSAGELOG_CH_ID = 305073652280590339L;
    public static final long SERVERLOG_CH_ID = 217456105679224846L;
    public static final long DEV_CH_ID = 269638376376893440L;

    //hub channels
    public static final long ERRORLOG_CH_ID = 346104666796589056L;
    public static final long BOTLOG_CH_ID = 346483682376286208L;
    public static final long BOTPM_CH_ID = 346104720903110656L;*/

    private long startTime;

    public CommandClientBuilder commandBuilder = new CommandClientBuilder();

    public static void main(String[] args) throws LoginException, InterruptedException {
        new TVBot();
    }

    public TVBot() throws LoginException, InterruptedException {

        instance = this;

        //instantiate config manager first as connect() relies on tokens
        configManager = new ConfigManager(this);
        commandManager = new CommandManager(this);
        toggleManager = new ToggleManager(this);
        prefixes.add(TVBot.getPrefix());
        prefixes.add("t!");
        prefixes.add("!g");
        prefixes.add("--");
        prefixes.add(".");
        prefixes.add("?");

        connect();

        databaseManager = new DatabaseManager(this);
        //traktManager = new TraktManager(this);
        //scheduler = new Scheduler(this);
    }

    private void connect() throws LoginException, InterruptedException {
        //don't load external modules and don't attempt to create modules folder
        //Configuration.LOAD_EXTERNAL_MODULES = false;

        Optional<String> token = configManager.getTokenValue("botToken");
        if (!token.isPresent()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your bot's token in the config.");
            System.out.println("Exiting......");
            System.out.println("-------------------------------------");
            System.exit(0);
            return;
        }

        commandBuilder.setOwnerId("73463573900173312");
        commandBuilder.setPrefix(prefix);
        commandBuilder.setAlternativePrefix(prefixes.toString());
        commandBuilder.setGame(Game.watching("all of your messages. Type " + prefix + "help"));

        startTime = System.currentTimeMillis();


        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(token.get())
                .addEventListener(commandBuilder.build());
        jda = builder.build();
        Util.sendMessage(Channels.TEST_CH_ID.getChannel(), "omg");
    }

    /*
     * Message Central Choo Choo
     */
    /*@EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return; //ignore bot messages
        IMessage message = event.getMessage();
        IGuild guild = null;
        boolean isPrivate = message.getChannel().isPrivate();
        if (!isPrivate) guild = message.getGuild();
        String text = message.getContent();
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.matches()) {
            String baseCommand = matcher.group(1).toLowerCase();
            Optional<Command> command = registeredCommands.stream()
                    .filter(com -> com.getName().equalsIgnoreCase(baseCommand) || (com.getAliases() != null && com.getAliases().contains(baseCommand)))
                    .findAny();
            if (command.isPresent()) {
                Command cCommand = command.get();

                if (cCommand.getDescription() != null || message.getAuthor().getLongID() == CBACK_USR_ID) {
                    System.out.println("@" + message.getAuthor().getName() + " issued \"" + text + "\" in " +
                            (isPrivate ? ("@" + message.getAuthor().getName()) : guild.getName()));

                    String args = matcher.group(2);
                    String[] argsArr = args.isEmpty() ? new String[0] : args.split(" ");

                    List<Long> roleIDs = message.getAuthor().getRolesForGuild(guild).stream().map(role -> role.getLongID()).collect(Collectors.toList());

                    IUser author = message.getAuthor();
                    String content = message.getContent();

                    *//**
                     * If user has permission to run the command: Command executes and botlogs
                     *//*
                    if (cCommand.getPermissions() == null || !Collections.disjoint(roleIDs, cCommand.getPermissions())) {
                        Util.botLog(message);
                        cCommand.execute(message, content, argsArr, author, guild, roleIDs, isPrivate, client, this);
                    } else {
                        Util.simpleEmbed(message.getChannel(), "You don't have permission to perform this command.");
                    }
                }
            } else if (commandManager.getCommandValue(baseCommand) != null) {

                String response = commandManager.getCommandValue(baseCommand);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("``" + message.getAuthor().getDisplayName(guild) + "``\n").append(response);

                Util.sendMessage(message.getChannel(), stringBuilder.toString());

                Util.deleteMessage(message);
            }
            *//**
             * Forwards the random stuff people PM to the bot - to me
             *//*
        } else if (message.getChannel().isPrivate()) {
            EmbedObject embed = Util.buildBotPMEmbed(message, 1);
            Util.sendEmbed(client.getChannelByID(BOTPM_CH_ID), embed);
        } else {
            //below here are just regular chat messages
            censorMessages(message);
            censorLinks(message);

            *//**
             * Deletes messages/bans users for using too many @ mentions
             *//*
            boolean staffMember = message.getAuthor().hasRole(message.getClient().getRoleByID(TVRoles.STAFF.id));
            if (!staffMember && toggleState("limitmentions")) {
                if (Util.mentionsCount(message.getContent()) > 10) {
                    try {
                        guild.banUser(message.getAuthor(), "Mentioned more than 10 users in a message. Appeal at https://www.reddit.com/r/LoungeBan/", 0);
                        Util.simpleEmbed(message.getChannel(), message.getAuthor().getDisplayName(guild) + " was just banned for mentioning more than 10 users.");
                        Util.sendLog(message, "Banned " + message.getAuthor().getName() + "\n**Reason:** Doing too many @ mentions", Color.red);
                    } catch (Exception e) {
                        Util.reportHome(e);
                    }
                } else if (Util.mentionsCount(message.getContent()) > 5) {
                    Util.deleteMessage(message);
                }
            }

            //Increment message count if message was not a command
            databaseManager.getXP().addXP(message.getAuthor().getStringID(), 1);

            *//**
             * Messages containing my name go to botpms now too cuz im watching
             *//*
            if (message.getContent().toLowerCase().contains("cback")) {
                EmbedObject embed = Util.buildBotPMEmbed(message, 2);
                Util.sendEmbed(client.getChannelByID(BOTPM_CH_ID), embed);
            }
        }
    }*/

    public static TVBot getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /*public TraktManager getTraktManager() {
        return traktManager;
    }*/

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ToggleManager getToggleMangager() { return toggleManager; }

    public static JDAClient getClient() {
        return client;
    }

    public static String getPrefix() {
        return prefix;
    }

    public static Guild getHomeGuild() {
        return jda.getGuildById(Long.parseLong(configManager.getConfigValue("HOMESERVER_ID")));
    }

    public static Guild getGuild() {
        return jda.getGuildById("247394948331077632");
    }

    public String getUptime() {
        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = (totalSeconds / 3600);
        return (hours < 10 ? "0" + hours : hours) + "h " + (minutes < 10 ? "0" + minutes : minutes) + "m " + (seconds < 10 ? "0" + seconds : seconds) + "s";
    }

    /**
     * Checks for dirty words :o
     */
    /*public void censorMessages(Message message) {
        if (toggleState("censorwords")) {
            boolean homeGuild = message.getGuild().getLongID() == TVBot.HOMESERVER_GLD_ID;
            boolean staffChannel = message.getChannel().getCategory().getLongID() == 355901035597922304L || message.getChannel().getCategory().getLongID() == 355910636464504832L;
            boolean staffMember = message.getAuthor().hasRole(message.getClient().getRoleByID(TVRoles.STAFF.id));
            if (homeGuild && !staffChannel && !staffMember) {
                List<String> bannedWords = TVBot.getInstance().getConfigManager().getConfigArray("bannedWords");
                String content = message.getFormattedContent().toLowerCase();

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

                    IUser author = message.getAuthor();

                    EmbedBuilder bld = new EmbedBuilder();
                    bld
                            .withAuthorIcon(author.getAvatarURL())
                            .withAuthorName(Util.getTag(author))
                            .withDesc(message.getFormattedContent())
                            .withTimestamp(System.currentTimeMillis())
                            .withFooterText("Auto-deleted from #" + message.getChannel().getName());

                    Util.sendEmbed(message.getGuild().getChannelByID(MESSAGELOG_CH_ID), bld.withColor(Util.getBotColor()).build());

                    StringBuilder sBld = new StringBuilder().append("Your message has been automatically removed for containing a banned word. If this is an error, message a staff member.");
                    if (!word.isEmpty()) {
                        sBld
                                .append("\n\n")
                                .append(word);
                    }
                    Util.sendPrivateEmbed(author, sBld.toString());

                    messageCache.add(message.getLongID());
                    Util.deleteMessage(message);
                }
            }
        }
    }*/

    /**
     * Censor links
     *//*
    public void censorLinks(IMessage message) {
        if (toggleState("censorlinks")) {
            IUser author = message.getAuthor();

            boolean homeGuild = message.getGuild().getLongID() == TVBot.HOMESERVER_GLD_ID;
            boolean staffChannel = message.getChannel().getCategory().getLongID() == 355901035597922304L || message.getChannel().getCategory().getLongID() == 355910636464504832L;
            boolean staffMember = author.hasRole(message.getClient().getRoleByID(TVRoles.STAFF.id));

            boolean trusted = false;
            List<IRole> userRoles = author.getRolesForGuild(message.getGuild());
            int tPos = client.getRoleByID(TVRoles.TRUSTED.id).getPosition();
            for (IRole r : userRoles) {
                int rPos = r.getPosition();
                if (rPos >= tPos) {
                    trusted = true;
                    break;
                }
            }

            if (homeGuild && !staffChannel && !staffMember && !trusted) {
                String content = message.getFormattedContent().toLowerCase();
                List<String> linksFound = new ArrayList<>();

                LinkExtractor linkExtractor = LinkExtractor.builder().build();
                Iterable<LinkSpan> links = linkExtractor.extractLinks(content);
                if (links.iterator().hasNext()) {
                    for (LinkSpan l : links) {
                        String f = message.getContent().substring(l.getBeginIndex(), l.getEndIndex());
                        linksFound.add(f);
                    }
                }

                if (linksFound.size() >= 1) {
                    String collectedLinks = "";
                    for (String s : linksFound) {
                        collectedLinks += s + " ";
                    }

                    EmbedBuilder bld = new EmbedBuilder();
                    bld
                            .withAuthorIcon(author.getAvatarURL())
                            .withAuthorName(Util.getTag(author))
                            .withDesc(message.getFormattedContent())
                            .withTimestamp(System.currentTimeMillis())
                            .withFooterText("Auto-deleted from #" + message.getChannel().getName());

                    Util.sendEmbed(message.getGuild().getChannelByID(MESSAGELOG_CH_ID), bld.withColor(Util.getBotColor()).build());
                    Util.sendPrivateEmbed(author, "Your message has been automatically removed for containing a link. If this is an error, message a staff member.\n\n" + collectedLinks);
                    messageCache.add(message.getLongID());
                    Util.deleteMessage(message);
                }
            }
        }
    }*/

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
