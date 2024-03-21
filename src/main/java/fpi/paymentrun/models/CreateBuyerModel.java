package fpi.paymentrun.models;

import commons.enums.Currency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

@Builder
@Getter
@Setter
public class CreateBuyerModel {

    private String tag;
    private AdminUserModel adminUser;
    private CompanyModel company;
    private boolean acceptedTerms;
    private String ipAddress;
    private String baseCurrency;
    private List<String> supportedCurrencies;

    public static CreateBuyerModel.CreateBuyerModelBuilder defaultCreateBuyerModel() {
        return CreateBuyerModel.builder()
                .tag(RandomStringUtils.randomAlphabetic(5))
                .adminUser(AdminUserModel.defaultAdminUserModel().build())
                .company(CompanyModel.gbCompanyModel().build())
                .acceptedTerms(true)
                .ipAddress("127.0.0.1")
                .baseCurrency(Currency.GBP.name())
                .supportedCurrencies(List.of(Currency.GBP.name()));
    }
}
