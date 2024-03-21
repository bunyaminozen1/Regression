package fpi.paymentrun.models;

import commons.enums.Currency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class PaymentsModel {

    private String reference;
    private String externalRef;
    private String paymentRef;
    private PaymentAmountModel paymentAmount;
    private PaymentsSupplierModel supplier;

    public static PaymentsModel.PaymentsModelBuilder defaultSEPABankAccountPaymentsModel(final String iban,
                                                                                         final String bankIdentifierCode){
        return PaymentsModel.builder()
                .externalRef(String.format("ExtRef%s", RandomStringUtils.randomAlphabetic(5)))
                .paymentRef(String.format("PayRef%s", RandomStringUtils.randomAlphabetic(5)))
                .paymentAmount(PaymentAmountModel.builder()
                        .currency(Currency.GBP.name())
                        .amount(100)
                        .build())
                .reference(String.format("Ref%s", RandomStringUtils.randomAlphabetic(5)))
                .supplier(PaymentsSupplierModel.defaultSEPABankAccountSupplerModel(iban, bankIdentifierCode).build());
    }

    public static PaymentsModel.PaymentsModelBuilder defaultFasterPaymentsBankAccountPaymentsModel(final String accountNumber,
                                                                                                   final String sortCode){
        return PaymentsModel.builder()
                .externalRef(String.format("ExtRef%s", RandomStringUtils.randomAlphabetic(5)))
                .paymentRef(String.format("PayRef%s", RandomStringUtils.randomAlphabetic(5)))
                .paymentAmount(PaymentAmountModel.builder()
                        .currency(Currency.GBP.name())
                        .amount(100)
                        .build())
                .reference(String.format("Ref%s", RandomStringUtils.randomAlphabetic(5)))
                .supplier(PaymentsSupplierModel.defaultFasterPaymentsBankAccountSupplerModel(accountNumber, sortCode).build());
    }
}
