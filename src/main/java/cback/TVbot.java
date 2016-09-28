package cback;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.util.DiscordException;

public class TVbot {
    public IDiscordClient client;
    public DatabaseManager databaseManager;

    public static void main(String[] args) {
        new TVbot();
    }

    public TVbot() {
        connect();
        databaseManager = new DatabaseManager();
        client.getDispatcher().registerListener(this);
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

    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        System.out.println("Logged in.");
    }

    @EventSubscriber
    public void onDisconnectEvent(DiscordDisconnectedEvent event) {
        System.out.println("BOT DISCONNECTED");
        System.out.println("Reason: " + event.getReason());
    }
}
