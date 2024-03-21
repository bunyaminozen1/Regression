package opc.junit.admin.managedcards;

import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetManagedCardTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;

    @BeforeAll
    public static void Setup() throws InterruptedException {
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedCard_ConsumerProcessorCardProduct(final Currency currency) {

        final ManagedCardDetails managedCard = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                currency.name(), consumerAuthenticationToken);

        AdminService
                .getManagedCard(managedCard.getManagedCardId(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("prepaidCard.managedCard.processorCardProduct", equalTo(ManagedCardsHelper.getProcessorCardProduct(IdentityType.CONSUMER, currency)))
                .body("prepaidCard.managedCard.emiLicenseHolder", equalTo(EmiLicenseHolder.NON_EMI.name()));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedCard_CorporateProcessorCardProduct(final Currency currency) {

        final ManagedCardDetails managedCard = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId,
                currency.name(), corporateAuthenticationToken);

        AdminService
                .getManagedCard(managedCard.getManagedCardId(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("prepaidCard.managedCard.processorCardProduct", equalTo(ManagedCardsHelper.getProcessorCardProduct(IdentityType.CORPORATE, currency)))
                .body("prepaidCard.managedCard.emiLicenseHolder", equalTo(EmiLicenseHolder.NON_EMI.name()));
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