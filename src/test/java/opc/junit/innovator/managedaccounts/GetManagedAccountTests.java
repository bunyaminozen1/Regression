package opc.junit.innovator.managedaccounts;

import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.innovator.InnovatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetManagedAccountTests extends BaseManagedAccountsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;

    @BeforeAll
    public static void Setup() throws InterruptedException {
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccount_ConsumerProcessorAccountProduct(final Currency currency) {

        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountsProfileId, currency).build(),
                        secretKey, consumerAuthenticationToken);

        InnovatorService
                .getManagedAccount(managedAccountId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(managedAccountId))
                .body("owner.type", equalTo("consumers"))
                .body("processorAccountProduct", equalTo(ManagedAccountsHelper.getProcessorAccountProduct(currency)))
                .body("emiLicenseHolder", equalTo(EmiLicenseHolder.NON_EMI.name()));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccount_CorporateProcessorAccountProduct(final Currency currency) {

        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, currency).build(),
                        secretKey, corporateAuthenticationToken);

        InnovatorService
                .getManagedAccount(managedAccountId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(managedAccountId))
                .body("owner.type", equalTo("corporates"))
                .body("processorAccountProduct", equalTo(ManagedAccountsHelper.getProcessorAccountProduct(currency)))
                .body("emiLicenseHolder", equalTo(EmiLicenseHolder.NON_EMI.name()));
    }

    private static void consumerSetup() {
        final CreateConsumerModel consumerDetails = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        consumerAuthenticationToken = ConsumersHelper
                .createAuthenticatedVerifiedConsumer(consumerDetails, KycLevel.KYC_LEVEL_1, secretKey).getRight();
    }

    private static void corporateSetup() {
        CreateCorporateModel corporateDetails = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        corporateAuthenticationToken = CorporatesHelper
                .createAuthenticatedVerifiedCorporate(corporateDetails, secretKey).getRight();
    }
}