package cback.database.tv;

public class Airing {

    private String episodeID;
    private String showID;
    private long airingTime;
    private String episodeInfo;
    private boolean sentStatus;

    public Airing(String episodeID, String showID, long airingTime, String episodeInfo, boolean sentStatus) {
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

    public long getAiringTime() {
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
}
