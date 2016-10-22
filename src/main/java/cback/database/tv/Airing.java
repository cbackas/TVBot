package cback.database.tv;

public class Airing {

    private String episodeID;
    private String showID;
    private long airingTime;
    private String episodeInfo;
    private String messageID;

    public Airing(String episodeID, String showID, long airingTime, String episodeInfo, String messageID) {
        this.episodeID = episodeID;
        this.showID = showID;
        this.airingTime = airingTime;
        this.episodeInfo = episodeInfo;
        this.messageID = messageID;
    }

    public String getEpisodeID() {
        return episodeID;
    }

    public String getShowID() {
        return showID;
    }

    public long getAiringTime() {
        return airingTime;
    }

    public String getEpisodeInfo() {
        return episodeInfo;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }
}
