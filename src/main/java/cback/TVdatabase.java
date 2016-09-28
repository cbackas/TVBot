package cback;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesWrapper;

public class TVdatabase {
    TheTvdb theTvdb;

    public TVdatabase() {
        theTvdb = new TheTvdb("this is an api key");
    }

    public void getTVinfo(String showID) {
        try {
            retrofit2.Response<SeriesWrapper> response = theTvdb.series().series(83462, "en").execute();
            if (response.isSuccessful()) {
                Series series = response.body().data;
                System.out.println(series.seriesName + " is awesome!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
