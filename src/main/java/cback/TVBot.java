package cback;

import cback.eventFunctions.ChannelChange;
import cback.eventFunctions.MemberChange;
import cback.commands.*;
import cback.database.DatabaseManager;
import cback.eventFunctions.MessageChange;
import org.reflections.Reflections;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.Configuration;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cback.Util.getAvatar;
import static cback.Util.reportHome;

@SuppressWarnings("FieldCanBeLocal")
public class TVBot {

    private static TVBot instance;
    private static IDiscordClient client;

    private DatabaseManager databaseManager;
    private TraktManager traktManager;
    private static ConfigManager configManager;
    private CommandManager commandManager;
    private Scheduler scheduler;

    private List<String> botAdmins = new ArrayList<>();

    public static List<Command> registeredCommands = new ArrayList<>();
    static private String prefix = "!";
    public List<String> prefixes = new ArrayList<>();
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^!([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);

    public static final Long ANNOUNCEMENT_CHANNEL_ID = 345774506373021716l;
    public static final Long NEW_EPISODE_CHANNEL_ID = 263184398894104577l;
    public static final Long GENERAL_CHANNEL_ID = 192441520178200577l;
    public static final Long LOG_CHANNEL_ID = 217456105679224846l;

    private long startTime;

    public static void main(String[] args) {
        new TVBot();
    }

    public TVBot() {

        instance = this;

        //instantiate config manager first as connect() relies on tokens
        configManager = new ConfigManager(this);
        commandManager = new CommandManager(this);
        prefixes.add(TVBot.getPrefix());
        prefixes.add("t!");
        prefixes.add("!g");
        prefixes.add("--");
        prefixes.add(".");

        connect();
        client.getDispatcher().registerListener(this);
        client.getDispatcher().registerListener(new ChannelChange(this));
        client.getDispatcher().registerListener(new MemberChange(this));
        client.getDispatcher().registerListener(new MessageChange(this));

        databaseManager = new DatabaseManager(this);
        traktManager = new TraktManager(this);
        scheduler = new Scheduler(this);

        registerAllCommands();

        botAdmins.add("109109946565537792");
        botAdmins.add("148279556619370496");
        botAdmins.add("73416411443113984");
        botAdmins.add("144412318447435776");

    }

    private void connect() {
        //don't load external modules and don't attempt to create modules folder
        Configuration.LOAD_EXTERNAL_MODULES = false;

        Optional<String> token = configManager.getTokenValue("botToken");
        if (!token.isPresent()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your bot's token in the config.");
            System.out.println("Exiting......");
            System.out.println("-------------------------------------");
            System.exit(0);
            return;
        }

        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token.get());
        clientBuilder.setMaxReconnectAttempts(5);
        try {
            client = clientBuilder.login();
        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    /*
     * Message Central Choo Choo
     */
    @EventSubscriber
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
                System.out.println("@" + message.getAuthor().getName() + " issued \"" + text + "\" in " +
                        (isPrivate ? ("@" + message.getAuthor().getName()) : guild.getName()));

                String args = matcher.group(2);
                String[] argsArr = args.isEmpty() ? new String[0] : args.split(" ");

                List<Long> roleIDs = message.getAuthor().getRolesForGuild(guild).stream().map(role -> role.getLongID()).collect(Collectors.toList());

                IUser author = message.getAuthor();
                String content = message.getContent();

                Command cCommand = command.get();

                /*
                 * If user has permission to run the command: Command executes and botlogs
                 */
                //message.getChannel().setTypingStatus(true);
                if (cCommand.getPermissions() == null || !Collections.disjoint(roleIDs, cCommand.getPermissions())) {
                    cCommand.execute(message, content, argsArr, author, guild, roleIDs, isPrivate, client, this);
                    Util.botLog(message);
                    //message.getChannel().setTypingStatus(false);
                } else {
                    Util.simpleEmbed(message.getChannel(), "You don't have permission to perform this command.");
                    //message.getChannel().setTypingStatus(false);
                }
            } else if (commandManager.getCommandValue(baseCommand) != null) {

                String response = commandManager.getCommandValue(baseCommand);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("``" + message.getAuthor().getDisplayName(guild) + "``\n").append(response);

                Util.sendMessage(message.getChannel(), stringBuilder.toString());

                Util.deleteMessage(message);
            }
            /**
             * Forwards the random stuff people PM to the bot - to me
             */
        } else if (message.getChannel().isPrivate()) {
            EmbedBuilder bld = new EmbedBuilder()
                    .withColor(Util.getBotColor())
                    .withTimestamp(System.currentTimeMillis())
                    .withAuthorName(message.getAuthor().getName() + '#' + message.getAuthor().getDiscriminator())
                    .withAuthorIcon(getAvatar(message.getAuthor()))
                    .withDesc(message.getContent());

            Util.sendEmbed(client.getChannelByID(346104720903110656l), bld.build());
        } else {
            if (message.getMentions().size() > 10) {
                try {
                    guild.banUser(message.getAuthor(), 1);
                    Util.sendLog(message, "Banned " + message.getAuthor().getName() + "\n**Reason:** Doing too many @ mentions", Color.red);
                } catch (Exception e) {
                    reportHome(e);
                }
            } else if (message.getMentions().size() > 5) {
                Util.deleteMessage(message);
            }
            //Increment message count if message was not a command
            databaseManager.getXP().addXP(message.getAuthor().getStringID(), 1);
        }
    }

    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        System.out.println("Logged in.");
        client = event.getClient();

        startTime = System.currentTimeMillis();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TraktManager getTraktManager() {
        return traktManager;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public static IDiscordClient getClient() {
        return client;
    }

    public static String getPrefix() {
        return prefix;
    }

    public static IGuild getHomeGuild() { return client.getGuildByID(Long.parseLong(configManager.getConfigValue("HOMESERVER_ID")));}

    private void registerAllCommands() {
        new Reflections("cback.commands").getSubTypesOf(Command.class).forEach(commandImpl -> {
            try {
                Command command = commandImpl.newInstance();
                Optional<Command> existingCommand = registeredCommands.stream().filter(cmd -> cmd.getName().equalsIgnoreCase(command.getName())).findAny();
                if (!existingCommand.isPresent()) {
                    registeredCommands.add(command);
                    System.out.println("Registered command: " + command.getName());
                } else {
                    System.out.println("Attempted to register two commands with the same name: " + existingCommand.get().getName());
                    System.out.println("Existing: " + existingCommand.get().getClass().getName());
                    System.out.println("Attempted: " + commandImpl.getName());
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    public String getUptime() {
        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = (totalSeconds / 3600);
        return (hours < 10 ? "0" + hours : hours) + "h " + (minutes < 10 ? "0" + minutes : minutes) + "m " + (seconds < 10 ? "0" + seconds : seconds) + "s";
    }

    public static TVBot getInstance() {
        return instance;
    }

}
