package cback;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    Connection connection;

    public DatabaseManager() {
        initTables();
    }

    public Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + Util.botPath + "/showdata.db");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public void initTables() {
        try {
            // create a database connection
            Statement statement = getConnection().createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS airing (show_id TEXT PRIMARY KEY, time INT, episode TEXT);");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS showdata (show_id TEXT PRIMARY KEY, show_name TEXT, channel_id TEXT);");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public String getShowName(String showID) {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT show_name FROM showdata WHERE show_id = '" + showID + "';");
            if (rs.next()) {
                return rs.getString("show_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }

    public String getChannelID(String showID) {
        try {
        Statement statement = getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT channel_id FROM showdata WHERE show_id = '" + showID + "';");
        if (rs.next()) {
            return rs.getString("channel_id");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
        return "error";
    }

}
