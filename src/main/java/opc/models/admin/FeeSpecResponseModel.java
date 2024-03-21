package opc.models.admin;

import java.util.LinkedHashMap;
import java.util.List;

public class FeeSpecResponseModel {

    private String type;
    private List<LinkedHashMap<String, String>> flatAmount;
    private LinkedHashMap<String, String> percentage;

    public String getType() {
        return type;
    }

    public FeeSpecResponseModel setType(String type) {
        this.type = type;
        return this;
    }

    public List<LinkedHashMap<String, String>> getFlatAmount() {
        return flatAmount;
    }

    public FeeSpecResponseModel setFlatAmount(List<LinkedHashMap<String, String>> flatAmount) {
        this.flatAmount = flatAmount;
        return this;
    }

    public LinkedHashMap<String, String> getPercentage() {
        return percentage;
    }

    public FeeSpecResponseModel setPercentage(LinkedHashMap<String, String> percentage) {
        this.percentage = percentage;
        return this;
    }
}
