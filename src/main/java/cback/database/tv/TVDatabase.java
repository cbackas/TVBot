package cback.database.tv;

import cback.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TVDatabase {

    private DatabaseManager dbManager;

    public TVDatabase(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initTable();
    }

    private void initTable() {
        try {
            Statement statement = dbManager.getConnection().createStatement();

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS airing (episode_id TEXT PRIMARY KEY, show_id TEXT, time INT, episode_info TEXT, sent_status INT);");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS showdata (show_id TEXT PRIMARY KEY, show_name TEXT, network TEXT, channel_id TEXT);");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public Show getShow(String showID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("SELECT * FROM showdata WHERE show_id = ?;");
            statement.setString(1, showID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new Show(showID, rs.getString("show_name"), rs.getString("network"), rs.getString("channel_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Show> getShowsByChannel(String channelID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("SELECT * FROM showdata WHERE channel_id = ?;");
            statement.setString(1, channelID);
            ResultSet rs = statement.executeQuery();
            List<Show> shows = new ArrayList<>();
            while (rs.next()) {
                shows.add(new Show(rs.getString("show_id"), rs.getString("show_name"), rs.getString("network"), channelID));
            }
            return shows;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getShowIDs() {
        try {
            Statement statement = dbManager.getConnection().createStatement();
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
            PreparedStatement statement = dbManager.getConnection().prepareStatement("SELECT * FROM airing WHERE episode_id = ?;");
            statement.setString(1, episodeID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new Airing(episodeID, rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getBoolean("sent_status"));
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
            Statement statement = dbManager.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM airing WHERE sent_status = 0 ORDER BY time ASC LIMIT 100;");
            List<Airing> airings = new ArrayList<>();
            while (rs.next()) {
                airings.add(new Airing(rs.getString("episode_id"), rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getBoolean("sent_status")));
            }
            return airings;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets airings that have been announced but not deleted from database
     */
    public List<Airing> getOldAirings() {
        try {
            Statement statement = dbManager.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM airing WHERE sent_status = 1 ORDER BY time ASC;");
            List<Airing> airings = new ArrayList<>();
            while (rs.next()) {
                airings.add(new Airing(rs.getString("episode_id"), rs.getString("show_id"), rs.getInt("time"), rs.getString("episode_info"), rs.getBoolean("sent_status")));
            }
            return airings;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void insertAiring(String episodeID, String showID, int time, String episodeInfo, boolean sentStatus) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("INSERT OR IGNORE INTO airing VALUES (?,?,?,?,?);");
            statement.setString(1, episodeID);
            statement.setString(2, showID);
            statement.setInt(3, time);
            statement.setString(4, episodeInfo);
            statement.setInt(5, sentStatus ? 1 : 0);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes airing's new sent status to database
     * @param airing
     */
    public void updateAiringSentStatus(Airing airing) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE airing SET sent_status = ? WHERE episode_id= ?;");
            statement.setInt(1, airing.getSentStatus() ? 1 : 0);
            statement.setString(2, airing.getEpisodeID());
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertShowData(String showID, String showName, String network, String channelID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("REPLACE INTO showdata VALUES (?,?,?,?);");
            statement.setString(1, showID);
            statement.setString(2, showName);
            statement.setString(3, network);
            statement.setString(4, channelID);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateShowNetwork(Show show){
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE showdata SET network = ? WHERE show_id= ?;");
            statement.setString(1, show.getNetwork());
            statement.setString(2, show.getShowID());
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int deleteShow(String showID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("DELETE FROM showdata WHERE show_id = ?;");
            statement.setString(1, showID);
            return statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int deleteAiring(String episodeID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("DELETE FROM airing WHERE episode_id = ?;");
            statement.setString(1, episodeID);
            return statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
