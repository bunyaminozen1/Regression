package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentsResponseModel {

    private String id;
    private String status;
    private String externalRef;
    private String paymentRef;
    private PaymentAmountResponseModel paymentAmount;
    private String reference;
    private PaymentsSupplierResponseModel supplier;
}