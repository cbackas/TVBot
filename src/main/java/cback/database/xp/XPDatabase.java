package cback.database.xp;

import cback.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class XPDatabase {

    private DatabaseManager dbManager;

    public XPDatabase(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initTable();
    }

    private void initTable() {
        try {
            Statement statement = dbManager.getConnection().createStatement();

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS xpdata (user_id TEXT PRIMARY KEY, message_count INT);");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public UserXP getUserXP(String userID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("SELECT * FROM xpdata WHERE user_id = ?;");
            statement.setString(1, userID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new UserXP(userID, rs.getInt("message_count"));
            } else {
                createXPUser(userID);
                return new UserXP(userID, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getUserRank(String userID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement(
                    "SELECT COUNT(*) + 1 FROM (SELECT * FROM xpdata ORDER BY message_count DESC) WHERE message_count > (SELECT message_count FROM xpdata WHERE user_id = ?);"
            );
            statement.setString(1, userID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<UserXP> getTopUsers(int maxUsers) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("SELECT * FROM xpdata ORDER BY message_count DESC LIMIT ?;");
            statement.setInt(1, maxUsers);
            ResultSet rs = statement.executeQuery();
            List<UserXP> users = new ArrayList<>();
            while (rs.next()) {
                users.add(new UserXP(rs.getString("user_id"), rs.getInt("message_count")));
            }
            return users;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateUserXP(UserXP userXP) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE xpdata SET message_count = ? WHERE user_id = ?;");
            statement.setInt(1, userXP.getMessageCount());
            statement.setString(2, userXP.getUserID());
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addXP(String userID, int amount) {
        try {
            getUserXP(userID); //call getUserXP to ensure user exists
            PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE xpdata SET message_count = message_count + ? WHERE user_id = ?;");
            statement.setInt(1, amount);
            statement.setString(2, userID);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeXP(String userID, int amount) {
        try {
            UserXP user = getUserXP(userID); //call getUserXP to ensure user exists
            if (user.getMessageCount() - amount < 0) amount = user.getMessageCount(); //set xp to 0 if goes to negative
            PreparedStatement statement = dbManager.getConnection().prepareStatement("UPDATE xpdata SET message_count = message_count - ? WHERE user_id = ?;");
            statement.setInt(1, amount);
            statement.setString(2, userID);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createXPUser(String userID) {
        try {
            PreparedStatement statement = dbManager.getConnection().prepareStatement("INSERT OR IGNORE INTO xpdata VALUES (?,?);");
            statement.setString(1, userID);
            statement.setInt(2, 0);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
