package spi.openbanking.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PaymentModel {

    private String idempotencyId;
    private PayeeModel payee;
    private int amount;
    private String currency;
    private String reference;
    private String contextType;
}
