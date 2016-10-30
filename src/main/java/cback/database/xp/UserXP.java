package cback.database.xp;

import cback.TVBot;
import sx.blah.discord.handle.obj.IUser;

public class UserXP {

    private String userID;
    private IUser user;
    private int messageCount;

    public UserXP(String userID, int messageCount) {
        this.userID = userID;
        this.messageCount = messageCount;
    }

    public String getUserID() {
        return userID;
    }

    public IUser getUser() {
        if(user == null) user = TVBot.getInstance().getClient().getUserByID(userID);
        return user;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int count) {
        messageCount = count;
    }
}
