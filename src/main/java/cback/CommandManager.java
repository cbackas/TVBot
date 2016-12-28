package cback;

import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class CommandManager {

    private TVBot bot;

    private File commandFile;
    private JSONObject commandJson;

    private static Map<String, Object> defaultCommands = new HashMap<>();

    static {
        //Insert all default config values here. They will be added on startup if they do not exist.
    }

    public CommandManager(TVBot bot) {
        this.bot = bot;
        initConfig();
    }

    private void initConfig() {
        try {
            commandFile = new File(Util.botPath, "commands.json");
            if (commandFile.exists()) {
                JSONParser parser = new JSONParser();
                FileReader reader = new FileReader(commandFile);
                commandJson = (JSONObject) parser.parse(reader);
                reader.close();
                ensureDefaultsExist();
            } else {
                writedefaultCommands();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writedefaultCommands() {
        System.out.println("Config file does not exist, writing default config.");
        commandJson = new JSONObject();
        commandJson.putAll(defaultCommands);

        writeConfig();
    }

    private void ensureDefaultsExist() {
        boolean addedDefaults = false;
        for (String key : defaultCommands.keySet()) {
            if (!commandJson.containsKey(key)) {
                addedDefaults = true;
                commandJson.put(key, defaultCommands.get(key));
            }
        }
        if (addedDefaults) {
            System.out.println("Default config value(s) not found in file, adding them...");
            writeConfig();
        }
    }

    private void writeConfig() {
        String prettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(commandJson);
        try {
            FileWriter writer = new FileWriter(commandFile);
            writer.write(prettyPrint);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a command response from the file
     */
    public String getCommandValue(String key) {
        return (String) commandJson.get(key);
    }

    /**
     * Sets a command value and writes it to the file
     */
    public void setConfigValue(String key, Object value) {
        commandJson.put(key, value);
        writeConfig();
    }

    /**
     * Deletes a command from the file
     */
    public void removeConfigValue(String key) {
        commandJson.remove(key);
        writeConfig();
    }

    /**
     * Returns a list of commands
     */
    public String getCommandList() {

        StringBuilder stringBuilder = new StringBuilder();
        for (Object key : commandJson.keySet()) {
            stringBuilder.append("\n").append(((String) key));
        }
        return stringBuilder.toString();
    }
}
