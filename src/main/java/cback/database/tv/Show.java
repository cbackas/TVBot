package cback.database.tv;

public class Show {

    private String showID;
    private String showName;
    private String network;
    private String channelID;

    public Show(String showID, String showName, String network, String channelID) {
        this.showID = showID;
        this.showName = showName;
        this.network = network;
        this.channelID = channelID;
    }

    public String getShowID() {
        return showID;
    }

    public String getShowName() {
        return showName;
    }

    public String getNetwork() { return  network; }

    public String getChannelID() {
        return channelID;
    }

    public void setNetwork(String network) { this.network = network; }
}
