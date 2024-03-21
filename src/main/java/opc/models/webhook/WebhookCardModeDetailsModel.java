package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookCardModeDetailsModel {

    private LinkedHashMap<String, WebhookCurrencyAmountDetailsModel> prepaidModeDetails;
    private WebhookDebitCardDetailsModel debitModeDetails;

    public LinkedHashMap<String, WebhookCurrencyAmountDetailsModel> getPrepaidModeDetails() {
        return prepaidModeDetails;
    }

    public WebhookCardModeDetailsModel setPrepaidModeDetails(LinkedHashMap<String, WebhookCurrencyAmountDetailsModel> prepaidModeDetails) {
        this.prepaidModeDetails = prepaidModeDetails;
        return this;
    }

    public WebhookDebitCardDetailsModel getDebitModeDetails() {
        return debitModeDetails;
    }

    public WebhookCardModeDetailsModel setDebitModeDetails(WebhookDebitCardDetailsModel debitModeDetails) {
        this.debitModeDetails = debitModeDetails;
        return this;
    }
}
