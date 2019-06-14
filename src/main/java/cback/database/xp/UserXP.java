package cback.database.xp;

import cback.TVBot;
import net.dv8tion.jda.core.entities.Member;

public class UserXP {

    private String userID;
    private Member user;
    private int messageCount;
    private int rank = 0;

    public UserXP(String userID, int messageCount) {
        this.userID = userID;
        this.messageCount = messageCount;
    }

    public String getUserID() {
        return userID;
    }

    public Member getUser() {
        if(user == null) user = TVBot.getGuild().getMemberById(Long.parseLong(userID));
        return user;
    }

    public int getRank() {
        if(rank == 0) rank = TVBot.getInstance().getDatabaseManager().getXP().getUserRank(userID);
        return rank;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int count) {
        messageCount = count;
    }
}
