package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BuyerDeactivatedWebhookDataEventModel {
    private String actionDoneBy;
    private String buyerId;
    private String reasonCode;
}