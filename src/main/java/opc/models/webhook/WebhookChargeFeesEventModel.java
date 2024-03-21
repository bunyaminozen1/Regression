package opc.models.webhook;

public class WebhookChargeFeesEventModel {

    private WebhookChargeFeesDetailsModel chargeFee;
    private String publishedTimestamp;
    private String type;

    public WebhookChargeFeesDetailsModel getChargeFee() {
        return chargeFee;
    }

    public WebhookChargeFeesEventModel setChargeFee(WebhookChargeFeesDetailsModel chargeFee) {
        this.chargeFee = chargeFee;
        return this;
    }

    public String getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public WebhookChargeFeesEventModel setPublishedTimestamp(String publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
        return this;
    }

    public String getType() {
        return type;
    }

    public WebhookChargeFeesEventModel setType(String type) {
        this.type = type;
        return this;
    }
}
