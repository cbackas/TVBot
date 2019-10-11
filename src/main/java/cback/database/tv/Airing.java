package cback.database.tv;

public class Airing {

    private String episodeID;
    private String showID;
    private int airingTime;
    private String episodeInfo;
    private boolean sentStatus;

    public Airing(String episodeID, String showID, int airingTime, String episodeInfo, boolean sentStatus) {
        this.episodeID = episodeID;
        this.showID = showID;
        this.airingTime = airingTime;
        this.episodeInfo = episodeInfo;
        this.sentStatus = sentStatus;
    }

    public String getEpisodeID() {
        return episodeID;
    }

    public String getShowID() {
        return showID;
    }

    public int getAiringTime() {
        return airingTime;
    }

    public String getEpisodeInfo() {
        return episodeInfo;
    }

    public boolean getSentStatus() {
        return sentStatus;
    }

    public void setSentStatus(boolean sentStatus) {
        this.sentStatus = sentStatus;
    }

    public void setEpisodeID(String episodeID) {
        this.episodeID = episodeID;
    }

    public void setShowID(String showID) {
        this.showID = showID;
    }

    public void setAiringTime(int airingTime) {
        this.airingTime = airingTime;
    }

    public void setEpisodeInfo(String episodeInfo) {
        this.episodeInfo = episodeInfo;
    }
}
