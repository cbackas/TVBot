package cback.database;

import cback.ConfigManager;
import cback.CustomCommandManager;
import cback.TVBot;
import cback.database.tv.TVDatabase;
import cback.database.xp.XPDatabase;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {

    private Connection connection;
    private TVBot bot;

    private TVDatabase tvDatabase;
    private XPDatabase xpDatabase;

    public DatabaseManager(TVBot bot) {
        this.bot = bot;
        this.tvDatabase = new TVDatabase(this);
        this.xpDatabase = new XPDatabase(this);
    }

    public Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + ConfigManager.botPath + "/showdata.db");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public TVDatabase getTV() {
        return tvDatabase;
    }

    public XPDatabase getXP() {
        return xpDatabase;
    }
}
