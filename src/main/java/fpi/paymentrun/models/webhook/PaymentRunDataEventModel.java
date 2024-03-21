package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PaymentRunDataEventModel {
    private String createdAt;
    private String createdBy;
    private String description;
    private String id;
    private String paymentRunRef;
    private List<PaymentRunPaymentsEventModel> payments;
    private String status;
    private String tag;
}
