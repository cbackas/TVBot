package cback;

import cback.database.DatabaseManager;
import cback.eventFunctions.ChannelChange;
import cback.eventFunctions.CommandListenerImpl;
import cback.eventFunctions.MemberChange;
import cback.eventFunctions.MessageChange;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.*;

public class TVBot extends ListenerAdapter {

    private static TVBot instance;
    private JDA jda;

    private DatabaseManager databaseManager;
    private TraktManager traktManager;
    private ConfigManager configManager;
    private CustomCommandManager customCommandManager;
    private CommandClient commandClient;
    private ToggleManager toggleManager;
    private Scheduler scheduler;

    private long startTime;

    public static final String COMMAND_PREFIX = "!";

    public static final long CBACK_USR_ID = 73416411443113984L;

    public static final long UNSORTED_CAT_ID = 358043583355289600L;
    public static final long STAFF_CAT_ID = 355901035597922304L;
    public static final long INFO_CAT_ID = 355910636464504832L;
    public static final long DISCUSSION_CAT_ID = 355910667812995084L;
    public static final long FUN_CAT_ID = 358679449451102210L;
    public static final long CARDS_CAT_ID = 821587214026407966L;
    public static final long NEW_CAT_ID = 358043583355289600L;
    public static final long CLOSED_CAT_ID = 355904962200469504L;
    public static final long AF_CAT_ID = 358038418208587785L;
    public static final long GL_CAT_ID = 358038474894606346L;
    public static final long MR_CAT_ID = 358038505244327937L;
    public static final long SZ_CAT_ID = 358038532780195840L;

    public static void main(String[] args) throws LoginException, InterruptedException {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        new TVBot();
    }

    public TVBot() throws LoginException, InterruptedException {
        instance = this;

        //instantiate config manager first as connect() relies on tokens
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        customCommandManager = new CustomCommandManager(this);
        toggleManager = new ToggleManager(this);

        connect();

        traktManager = new TraktManager(this);
        scheduler = new Scheduler(this);
    }

    private void connect() throws LoginException, InterruptedException {
        //don't load external modules and don't attempt to create modules folder
        //Configuration.LOAD_EXTERNAL_MODULES = false;

        Optional<String> token = configManager.getTokenValue("botToken");
        if (token.isEmpty()) {
            Util.getLogger().error("-------------------------------------");
            Util.getLogger().error("Insert your bot's token in the config.");
            Util.getLogger().error("Exiting......");
            Util.getLogger().error("-------------------------------------");
            System.exit(0);
            return;
        }

        var commandClientBuilder = new CommandClientBuilder()
                .setOwnerId(String.valueOf(CBACK_USR_ID))
                .setPrefix(COMMAND_PREFIX)
                .setActivity(Activity.watching("all of your messages. Type " + COMMAND_PREFIX + "help"))
                .useHelpBuilder(false)
                .setListener(new CommandListenerImpl());

        new Reflections("cback.commands").getSubTypesOf(Command.class).forEach(commandImpl -> {
            try {

                Command command = commandImpl.getDeclaredConstructor().newInstance();
                commandClientBuilder.addCommand(command);
                Util.getLogger().info("Registered Command: " + command.getName());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        this.commandClient = commandClientBuilder.build();

        this.jda = JDABuilder.createDefault(token.get())
                .setChunkingFilter(ChunkingFilter.include(Long.parseLong(configManager.getConfigValue("HOMESERVER_ID")))) // enable member chunking for the lounge
                .setMemberCachePolicy(MemberCachePolicy.ALL) // ignored if chunking enabled
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(commandClient)
                .addEventListeners(this)
                .addEventListeners(new ChannelChange(this))
                .addEventListeners(new MemberChange(this))
                .addEventListeners(new MessageChange(this))
                .addEventListeners(new CommandListener())
                .build()
                .awaitReady();

        startTime = System.currentTimeMillis();
    }

    @Override
    public void onReady(ReadyEvent event) {
        Util.getLogger().info("======READY======");
    }

    public static TVBot getInstance() {
        return instance;
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

    public CustomCommandManager getCustomCommandManager() {
        return customCommandManager;
    }

    public ToggleManager getToggleManager() {
        return toggleManager;
    }

    public JDA getClient() {
        return jda;
    }

    public CommandClient getCommandClient() {
        return commandClient;
    }

    public Guild getHomeGuild() {
        return jda.getGuildById(Long.parseLong(configManager.getConfigValue("HOMESERVER_ID")));
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
    public boolean getToggleState(String toggleKey) {
        return toggleManager.getToggleValue(toggleKey);
    }
}
