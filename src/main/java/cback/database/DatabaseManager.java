package cback.database;

import cback.TVBot;
import cback.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private Connection connection;
    private TVBot bot;

    public DatabaseManager(TVBot bot) {
        this.bot = bot;
        initTables();
    }

    private Connection getConnection() {
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

    private void initTables() {
        try {
            // create a database connection
            Statement statement = getConnection().createStatement();

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS airing (episode_id TEXT PRIMARY KEY, show_id TEXT, time INT, episode_info TEXT, message_id TEXT);");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS showdata (show_id TEXT PRIMARY KEY, show_name TEXT, channel_id TEXT);");
            //statement.executeUpdate("CREATE TABLE IF NOT EXISTS xpdata (user_id TEXT PRIMARY KEY, message_count INT");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }



    public Show getShow(String showID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM showdata WHERE show_id = ?;");
            statement.setString(1, showID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new Show(showID, rs.getString("show_name"), rs.getString("channel_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Show> getShowsByChannel(String channelID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM showdata WHERE channel_id = ?;");
            statement.setString(1, channelID);
            ResultSet rs = statement.executeQuery();
            List<Show> shows = new ArrayList<>();
            while (rs.next()) {
                shows.add(new Show(rs.getString("show_id"), rs.getString("show_name"), channelID));
            }
            return shows;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getShowIDs() {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT show_id FROM showdata;");
            List<String> showids = new ArrayList<>();
            while (rs.next()) {
                showids.add(rs.getString("show_id"));
            }
            return showids;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Airing getAiring(String episodeID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM airing WHERE episode_id = ?;");
            statement.setString(1, episodeID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new Airing(episodeID, rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getString("message_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets airings that have not been announced
     */
    public List<Airing> getNewAirings() {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM airing WHERE message_id = 'NONE' ORDER BY time ASC LIMIT 30;");
            List<Airing> airings = new ArrayList<>();
            while (rs.next()) {
                airings.add(new Airing(rs.getString("episode_id"), rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getString("message_id")));
            }
            return airings;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets airings that have been announced but not deleted
     */
    public List<Airing> getOldAirings() {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM airing WHERE NOT message_id = 'NONE' AND NOT message_id = 'DELETED' ORDER BY time ASC LIMIT 30;");
            List<Airing> airings = new ArrayList<>();
            while (rs.next()) {
                airings.add(new Airing(rs.getString("episode_id"), rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getString("message_id")));
            }
            return airings;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets airings that have been deleted but not wiped from the database
     */
    public List<Airing> getDeletedAirings() {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM airing WHERE message_id = 'DELETED';");
            List<Airing> airings = new ArrayList<>();
            while (rs.next()) {
                airings.add(new Airing(rs.getString("episode_id"), rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getString("message_id")));
            }
            return airings;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void insertAiring(String episodeID, String showID, int time, String episodeInfo, String messageID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("INSERT OR IGNORE INTO airing VALUES (?,?,?,?,?);");
            statement.setString(1, episodeID);
            statement.setString(2, showID);
            statement.setInt(3, time);
            statement.setString(4, episodeInfo);
            statement.setString(5, messageID);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAiringMessage(Airing airing) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("UPDATE airing SET message_id= ? WHERE episode_id= ?;");
            statement.setString(1, airing.getMessageID());
            statement.setString(2, airing.getEpisodeID());
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertShowData(String showID, String showName, String channelID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("REPLACE INTO showdata VALUES (?,?,?);");
            statement.setString(1, showID);
            statement.setString(2, showName);
            statement.setString(3, channelID);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int deleteShow(String showID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("DELETE FROM showdata WHERE show_id = ?;");
            statement.setString(1, showID);
            return statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int deleteAiring(String episodeID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("DELETE FROM airing WHERE episode_id = ?;");
            statement.setString(1, episodeID);
            return statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}
