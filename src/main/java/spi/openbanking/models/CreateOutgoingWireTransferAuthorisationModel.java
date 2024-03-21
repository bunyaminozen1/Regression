package spi.openbanking.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class CreateOutgoingWireTransferAuthorisationModel {

    private PayerModel payer;
    private List<PaymentModel> payments;
    private String paymentDate;
    private String callbackUrl;
    private String state;
}
