package cback.database.xp;

import cback.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class XPDatabase {

    private DatabaseManager dbManager;

    public XPDatabase(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initTable();
    }

    public void initTable() {
        try {
            Statement statement = dbManager.getConnection().createStatement();

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS xpdata (user_id TEXT PRIMARY KEY, message_count INT);");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public UserXP getUserXP(String userID){
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("SELECT * FROM xpdata WHERE user_id = ?;");
            statement.setString(1, userID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new UserXP(userID, rs.getInt("message_count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateUserXP(UserXP userXP){
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE xpdata SET message_count = ? WHERE user_id = ?;");
            statement.setInt(1, userXP.getMessageCount());
            statement.setString(2, userXP.getUserID());
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addXP(String userID){
        try {
            if(getUserXP(userID) == null){
                addXPUser(userID);
            } else{
                PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE xpdata SET message_count = message_count + 1 WHERE user_id = ?;");
                statement.setString(1, userID);
                statement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addXPUser(String userID){
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("INSERT OR IGNORE INTO xpdata VALUES (?,?);");
            statement.setString(1, userID);
            statement.setInt(2, 1);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
