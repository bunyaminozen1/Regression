package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.models.multi.beneficiaries.BankAccountDetailsModel;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class PaymentsSupplierModel {
    private String name;
    private String address;
    private String bankName;
    private String bankAddress;
    private String bankCountry;
    private BankAccountDetailsModel bankAccountDetails;

    public static PaymentsSupplierModel.PaymentsSupplierModelBuilder defaultSEPABankAccountSupplerModel(final String iban,
                                                                                                        final String bankIdentifierCode) {
        return PaymentsSupplierModel.builder()
                .name(RandomStringUtils.randomAlphabetic(5))
                .address(RandomStringUtils.randomAlphabetic(5))
                .bankName(RandomStringUtils.randomAlphabetic(5))
                .bankAddress(RandomStringUtils.randomAlphabetic(5))
                .bankCountry("MT")
                .bankAccountDetails(BankAccountDetailsModel.SEPABankAccountDetails(iban, bankIdentifierCode).build());
    }

    public static PaymentsSupplierModel.PaymentsSupplierModelBuilder defaultFasterPaymentsBankAccountSupplerModel(final String accountNumber,
                                                                                                                  final String sortCode) {
        return PaymentsSupplierModel.builder()
                .name(RandomStringUtils.randomAlphabetic(5))
                .address(RandomStringUtils.randomAlphabetic(5))
                .bankName(RandomStringUtils.randomAlphabetic(5))
                .bankAddress(RandomStringUtils.randomAlphabetic(5))
                .bankCountry("MT")
                .bankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(accountNumber, sortCode).build());
    }
}
