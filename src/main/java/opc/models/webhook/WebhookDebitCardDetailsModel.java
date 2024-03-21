package opc.models.webhook;

import java.util.LinkedHashMap;
import java.util.List;

public class WebhookDebitCardDetailsModel {

    private List<LinkedHashMap<String, String>> availableToSpend;
    private String parentManagedAccountId;

    public List<LinkedHashMap<String, String>> getAvailableToSpend() {
        return availableToSpend;
    }

    public WebhookDebitCardDetailsModel setAvailableToSpend(List<LinkedHashMap<String, String>> availableToSpend) {
        this.availableToSpend = availableToSpend;
        return this;
    }

    public String getParentManagedAccountId() {
        return parentManagedAccountId;
    }

    public WebhookDebitCardDetailsModel setParentManagedAccountId(String parentManagedAccountId) {
        this.parentManagedAccountId = parentManagedAccountId;
        return this;
    }
}
