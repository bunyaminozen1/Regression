package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
@Setter
public class CreatePaymentRunModel {

    private String paymentRunRef;
    private String tag;
    private String description;
    private List<PaymentsModel> payments;

    public static CreatePaymentRunModel.CreatePaymentRunModelBuilder  defaultCreatePaymentRunSEPABankAccountModel(final String iban,
                                                                                                                 final String bankIdentifierCode) {
        return CreatePaymentRunModel.builder()
                .paymentRunRef(String.format("PayRunRef%s", RandomStringUtils.randomAlphabetic(5)))
                .tag(String.format("Tag%s", RandomStringUtils.randomAlphabetic(5)))
                .description(String.format("Description%s", RandomStringUtils.randomAlphabetic(5)))
                .payments(List.of(PaymentsModel.defaultSEPABankAccountPaymentsModel(iban, bankIdentifierCode).build()));
    }

    public static CreatePaymentRunModel.CreatePaymentRunModelBuilder defaultCreatePaymentRunFasterPaymentsBankAccountModel(final String accountNumber,
                                                                                                                           final String sortCode) {
        return CreatePaymentRunModel.builder()
                .paymentRunRef(String.format("PayRunRef%s", RandomStringUtils.randomAlphabetic(5)))
                .tag(String.format("Tag%s", RandomStringUtils.randomAlphabetic(5)))
                .description(String.format("Description%s", RandomStringUtils.randomAlphabetic(5)))
                .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(accountNumber, sortCode).build()));
    }
}
