package cback;

import cback.ServerFunctions.MemberLog;
import cback.ServerFunctions.MutePermissions;
import cback.commands.*;
import cback.database.DatabaseManager;
import cback.database.Show;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.modules.Configuration;
import sx.blah.discord.util.DiscordException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVBot {
    private IDiscordClient client;
    private DatabaseManager databaseManager;
    private TraktManager traktManager;
    private Scheduler scheduler;
    private List<String> botAdmins = new ArrayList<>();

    private List<Command> registeredCommands = new ArrayList<>();

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^!([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);
    public static final String ANNOUNCEMENT_CHANNEL_ID = "227852239769698304";
    public static final String GENERAL_CHANNEL_ID = "192441520178200577";


    public static void main(String[] args) {
        new TVBot();
    }

    public TVBot() {

        connect();
        client.getDispatcher().registerListener(this);
        client.getDispatcher().registerListener(new MutePermissions());
        client.getDispatcher().registerListener(new MemberLog());

        databaseManager = new DatabaseManager(this);
        traktManager = new TraktManager(this);
        scheduler = new Scheduler(this);

        registerCommand(new CommandHelp());
        registerCommand(new CommandAddShow());
        registerCommand(new CommandRemoveShow());
        registerCommand(new CommandAddLog());
        registerCommand(new CommandMute());

        botAdmins.add("109109946565537792");
        botAdmins.add("148279556619370496");
        botAdmins.add("73416411443113984");
        botAdmins.add("144412318447435776");

    }

    private void connect() {
        //don't load external modules and don't attempt to create modules folder
        Configuration.LOAD_EXTERNAL_MODULES = false;

        Optional<String> token = Util.getToken("bottoken.txt");
        if (!token.isPresent()) {
            System.out.println("-------------------------------------");
            System.out.println("Insert your bot's token in bottoken.txt");
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
            Optional<Command> command = registeredCommands.stream().filter(com -> com.getName().equalsIgnoreCase(baseCommand)).findFirst();
            if (command.isPresent()) {
                System.out.println("@" + message.getAuthor().getName() + " issued \"" + text + "\" in " +
                        (isPrivate ? ("@" + message.getAuthor().getName()) : guild.getName()));

                List<IUser> mentionsU = message.getMentions();
                List<IRole> mentionsG = message.getRoleMentions();
                String finalText = "@" + message.getAuthor().getName() + " issued \"" + text + "\" in " + message.getChannel().mention();
                if (mentionsU.isEmpty() && mentionsG.isEmpty()) {
                    Util.sendMessage(client.getChannelByID("231499461740724224"), finalText);
                } else {
                    for (IUser u : mentionsU) {
                        String displayName = "\\@" + u.getDisplayName(message.getGuild());
                        finalText = finalText.replace(u.mention(false), displayName).replace(u.mention(true), displayName);
                    }
                    for (IRole g : mentionsG) {
                        String displayName = "\\@" + g.getName();
                        finalText = finalText.replace(g.mention(), displayName).replace(g.mention(), displayName);
                    }
                    Util.sendMessage(event.getClient().getChannelByID("231499461740724224"), finalText);
                }

                String args = matcher.group(2);
                String[] argsArr = args.isEmpty() ? new String[0] : args.split(" ");
                command.get().execute(this, client, argsArr, guild, message, isPrivate);
            }

        }
    }

    @EventSubscriber
    public void onDeleteChannelEvent(ChannelDeleteEvent event) {
        List<Show> shows = getDatabaseManager().getShowsByChannel(event.getChannel().getID());
        if (shows != null) {
            shows.forEach(show -> {
                if (getDatabaseManager().deleteShow(show.getShowID()) > 0) {
                    System.out.println("Channel Deleted: Removed show " + show.getShowName() + " from database automatically.");
                }
            });
        }
    }

    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        System.out.println("Logged in.");
    }

    @EventSubscriber
    public void onDisconnectEvent(DiscordDisconnectedEvent event) {
        System.out.println("BOT DISCONNECTED");
        System.out.println("Reason: " + event.getReason());
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TraktManager getTraktManager() {
        return traktManager;
    }

    public IDiscordClient getClient() {
        return client;
    }

    public List<String> getBotAdmins() {
        return botAdmins;
    }

    public void registerCommand(Command command) {
        if (!registeredCommands.contains(command)) {
            registeredCommands.add(command);
            System.out.println("Registered command: " + command.getName());
        }
    }

}
