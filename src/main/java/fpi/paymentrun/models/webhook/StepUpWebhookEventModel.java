package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StepUpWebhookEventModel {
    private String type;
    private StepUpWebhookDataEventModel data;
}
