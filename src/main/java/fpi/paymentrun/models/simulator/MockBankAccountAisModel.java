package fpi.paymentrun.models.simulator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class MockBankAccountAisModel {
    private final String authUrlRequestId;
    private final String accountNumber;
    private final String sortCode;
    private final String currency;

    public static MockBankAccountAisModel.MockBankAccountAisModelBuilder defaultMockBankAccountAisModel(final String authUrlRequestId){
        return MockBankAccountAisModel.builder()
                .accountNumber(RandomStringUtils.randomNumeric(8))
                .sortCode(RandomStringUtils.randomNumeric(6))
                .currency("GBP")
                .authUrlRequestId(authUrlRequestId);
    }
}
