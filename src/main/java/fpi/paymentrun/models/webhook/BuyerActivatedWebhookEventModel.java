package fpi.paymentrun.models.webhook;

import fpi.paymentrun.models.webhook.BuyerActivatedWebhookDataEventModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BuyerActivatedWebhookEventModel {
    private String type;
    private BuyerActivatedWebhookDataEventModel data;
}
