package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentAmountResponseModel {

    private String currency;
    private int amount;
}