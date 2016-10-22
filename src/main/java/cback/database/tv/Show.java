package cback.database.tv;

public class Show {

    private String showID;
    private String showName;
    private String channelID;

    public Show(String showID, String showName, String channelID) {
        this.showID = showID;
        this.showName = showName;
        this.channelID = channelID;
    }

    public String getShowID() {
        return showID;
    }

    public String getShowName() {
        return showName;
    }

    public String getChannelID() {
        return channelID;
    }
}
