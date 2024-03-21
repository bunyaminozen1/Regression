package opc.models.multi.managedcards;

import java.util.LinkedHashMap;

public class SpendLimitResponseModel {
    private LinkedHashMap<String, String> value;
    private String interval;

    public LinkedHashMap<String, String> getValue() {
        return value;
    }

    public SpendLimitResponseModel setValue(LinkedHashMap<String, String> value) {
        this.value = value;
        return this;
    }

    public String getInterval() {
        return interval;
    }

    public SpendLimitResponseModel setInterval(String interval) {
        this.interval = interval;
        return this;
    }
}
