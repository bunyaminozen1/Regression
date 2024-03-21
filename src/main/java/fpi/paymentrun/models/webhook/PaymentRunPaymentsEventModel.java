package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class PaymentRunPaymentsEventModel {
    private String id;
    private String externalRef;
    private LinkedHashMap<String, String> paymentAmount;
    private String paymentRef;
    private String reference;
    private String status;
    private SupplierEventModel supplier;
}
