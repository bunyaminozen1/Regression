package fpi.paymentrun.models.innovator;

import commons.enums.Currency;
import commons.models.innovator.IdentityProfileAuthenticationModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.CompanyType;
import opc.enums.opc.OwtType;

import java.util.List;

@Builder
@Getter
@Setter
public class CreatePaymentRunProfilesModel {

    private List<String> tag;
    private EmbedderModel embedder;
    private CorporateModel corporate;
    private ManagedAccountModel managedAccount;
    private OwtModel owt;
    private OpenBankingModel openBanking;

    public static CreatePaymentRunProfilesModel defaultCreatePaymentRunProgrammesModel(final String innovatorName) {
        return CreatePaymentRunProfilesModel.builder()
                .embedder(EmbedderModel.builder().companyRegistrationName(innovatorName).build())
                .corporate(CorporateModel.builder()
                        .companyType(List.of(CompanyType.SOLE_TRADER.name(), CompanyType.LLC.name(), CompanyType.NON_PROFIT_ORGANISATION.name(),
                                CompanyType.PUBLIC_LIMITED_COMPANY.name(), CompanyType.LIMITED_LIABILITY_PARTNERSHIP.name()))
                        .baseUrl("https://www.fake.com")
                        .baseCurrency(Currency.GBP.name())
                        .accountInformationFactors(IdentityProfileAuthenticationModel.SmsOtpAccountInfoIdentityProfileAuthenticationScheme())
                        .paymentInitiationFactors(IdentityProfileAuthenticationModel.DefaultSmsIdentityProfileAuthenticationScheme())
                        .beneficiaryManagementFactors(IdentityProfileAuthenticationModel.DefaultSmsIdentityProfileAuthenticationScheme())
                        .build())
                .managedAccount(ManagedAccountModel.builder().currency(List.of(Currency.GBP.name())).build())
                .owt(OwtModel.builder().supportedType(List.of(OwtType.FASTER_PAYMENTS.name())).build())
                .build();
    }
}
