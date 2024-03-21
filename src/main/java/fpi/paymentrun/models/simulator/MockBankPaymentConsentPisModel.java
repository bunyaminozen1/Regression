package fpi.paymentrun.models.simulator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class MockBankPaymentConsentPisModel {
    private final String transferId;
    private final String isoStatusCode;
}
