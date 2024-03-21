package fpi.paymentrun.models.webhook;

import fpi.paymentrun.models.webhook.BuyerDeactivatedWebhookDataEventModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BuyerDeactivatedWebhookEventModel {
    private String type;
    private BuyerDeactivatedWebhookDataEventModel data;
}
