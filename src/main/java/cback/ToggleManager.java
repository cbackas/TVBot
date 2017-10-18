package cback;

import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.util.*;

public class ToggleManager {

    private TVBot bot;

    private File toggleFile;
    private JSONObject toggleJson;

    private static Map<String, Object> defaultToggles = new HashMap<>();

    static {
        //Insert all default config values here. They will be added on startup if they do not exist.
        defaultToggles.put("autosecure", "false");
        defaultToggles.put("censorwords", "false");
        defaultToggles.put("censorlinks", "false");
    }

    public ToggleManager(TVBot bot) {
        this.bot = bot;
        initConfig();
    }

    private void initConfig() {
        try {
            toggleFile = new File(botPath, "toggles.json");
            if (toggleFile.exists()) {
                JSONParser parser = new JSONParser();
                FileReader reader = new FileReader(toggleFile);
                toggleJson = (JSONObject) parser.parse(reader);
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
        toggleJson = new JSONObject();
        toggleJson.putAll(defaultToggles);

        writeConfig();
    }

    private void ensureDefaultsExist() {
        boolean addedDefaults = false;
        for (String key : defaultToggles.keySet()) {
            if (!toggleJson.containsKey(key)) {
                addedDefaults = true;
                toggleJson.put(key, defaultToggles.get(key));
            }
        }
        if (addedDefaults) {
            System.out.println("Default config value(s) not found in file, adding them...");
            writeConfig();
        }
    }

    private void writeConfig() {
        String prettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(toggleJson);
        try {
            FileWriter writer = new FileWriter(toggleFile);
            writer.write(prettyPrint);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns true if a toggle is true
     */
    public Boolean getToggleValue(String key) {
        String in = (String) toggleJson.get(key);
        if (in.equals("true")) {
            return true;
        } else if (in.equals("false")) {
            return false;
        }
        return null;
    }

    /**
     * Sets a command value and writes it to the file
     */
    public void toggleToggleValue(String key) {
        if (getToggleValue(key)) {
            toggleJson.put(key, "false");
        } else {
            toggleJson.put(key, "true");
        }
        writeConfig();
    }

    /**
     * Returns a list of commands
     */
    public List<String> getToggleList() {
        List<String> toggles = new ArrayList<>();
        for (Object key : toggleJson.keySet()) {
            toggles.add((String) key);
        }
        return toggles;
    }

    /**
     * Bot path stuff
     */
    public static File botPath;

    static {
        try {
            botPath = new File(TVBot.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
