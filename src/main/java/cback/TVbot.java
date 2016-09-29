package cback;

import cback.commands.Command;
import cback.commands.CommandAddLog;
import cback.commands.CommandAddShow;
import cback.commands.CommandHelp;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVbot {
    private IDiscordClient client;
    private DatabaseManager databaseManager;
    private TraktHandler traktHandler;

    private List<Command> registeredCommands = new ArrayList<>();
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^!([^\\s]+) ?(.*)", Pattern.CASE_INSENSITIVE);



    public static void main(String[] args) {
        new TVbot();
    }

    public TVbot() {
            connect();
            databaseManager = new DatabaseManager(this);
            traktHandler = new TraktHandler(this);
            client.getDispatcher().registerListener(this);

            registerCommand(new CommandHelp());
            registerCommand(new CommandAddShow());
            registerCommand(new CommandAddLog());

        }

    private void connect() {
        ClientBuilder clientBuilder = new ClientBuilder(); //Creates the ClientBuilder instance
        clientBuilder.withToken("MjI5NzAxNjg1OTk4NTE4Mjc0.CsnF6g.M-igodHKI-wDVUzlotFMmz9xjsY"); //Token stored in properties.txt
        clientBuilder.setMaxReconnectAttempts(5);
        try {
            client = clientBuilder.login();
        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    private void scheduler() {

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
            if(command.isPresent()) {
                System.out.println("@" + message.getAuthor().getName() + " issued \"" + text + "\" in " +
                        (isPrivate ? ("@" + message.getAuthor().getName()) : guild.getName()));

                String args = matcher.group(2);
                String[] argsArr = args.isEmpty() ? new String[0] : args.split(" ");
                command.get().execute(this, client, argsArr, guild, message, isPrivate);
            }

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

    public TraktHandler getTraktHandler() {
        return traktHandler;
    }

    public void registerCommand(Command command) {
        if (!registeredCommands.contains(command)) {
            registeredCommands.add(command);
            System.out.println("Registered command: " + command.getName());
        }
    }

}
