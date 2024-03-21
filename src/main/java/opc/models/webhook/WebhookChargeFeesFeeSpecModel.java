package opc.models.webhook;

import java.util.LinkedHashMap;
import java.util.List;

public class WebhookChargeFeesFeeSpecModel {
    private String type;
    private List<LinkedHashMap<String, String>> flatAmount;
    private LinkedHashMap<String, String> percentage;

    public String getType() {
        return type;
    }

    public WebhookChargeFeesFeeSpecModel setType(String type) {
        this.type = type;
        return this;
    }

    public List<LinkedHashMap<String, String>> getFlatAmount() {
        return flatAmount;
    }

    public WebhookChargeFeesFeeSpecModel setFlatAmount(List<LinkedHashMap<String, String>> flatAmount) {
        this.flatAmount = flatAmount;
        return this;
    }

    public LinkedHashMap<String, String> getPercentage() {
        return percentage;
    }

    public WebhookChargeFeesFeeSpecModel setPercentage(LinkedHashMap<String, String> percentage) {
        this.percentage = percentage;
        return this;
    }
}
