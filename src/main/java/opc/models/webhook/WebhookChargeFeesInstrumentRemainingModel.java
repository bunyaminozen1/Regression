package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookChargeFeesInstrumentRemainingModel {
    public String availableRemaining;
    public String reservedRemaining;
    public String pendingRemaining;
    public LinkedHashMap<String, String> feeRemaining;

    public String getAvailableRemaining() {
        return availableRemaining;
    }

    public WebhookChargeFeesInstrumentRemainingModel setAvailableRemaining(String availableRemaining) {
        this.availableRemaining = availableRemaining;
        return this;
    }

    public String getReservedRemaining() {
        return reservedRemaining;
    }

    public WebhookChargeFeesInstrumentRemainingModel setReservedRemaining(String reservedRemaining) {
        this.reservedRemaining = reservedRemaining;
        return this;
    }

    public String getPendingRemaining() {
        return pendingRemaining;
    }

    public WebhookChargeFeesInstrumentRemainingModel setPendingRemaining(String pendingRemaining) {
        this.pendingRemaining = pendingRemaining;
        return this;
    }

    public LinkedHashMap<String, String> getFeeRemaining() {
        return feeRemaining;
    }

    public WebhookChargeFeesInstrumentRemainingModel setFeeRemaining(LinkedHashMap<String, String> feeRemaining) {
        this.feeRemaining = feeRemaining;
        return this;
    }
}
