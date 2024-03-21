package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentRunPaymentWebhookEventModel {
    private String type;
    private PaymentRunPaymentDataEventModel data;
}
