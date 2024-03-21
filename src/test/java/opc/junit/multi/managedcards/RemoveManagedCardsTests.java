package opc.junit.multi.managedcards;

import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static opc.enums.opc.IdentityType.CONSUMER;
import static opc.enums.opc.IdentityType.CORPORATE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class RemoveManagedCardsTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void RemoveManagedCard_PrepaidCorporate_Success() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedCard_PrepaidConsumer_Success() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedCard_DebitCorporate_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedCard_DebitConsumer_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedCard_BlockedInstrument_Success() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedCard_AlreadyDestroyed_InstrumentAlreadyRemoved() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REMOVED"));
    }

    @Test
    public void RemoveManagedCard_InvalidApiKey_Unauthorised(){
        ManagedCardsService.removeManagedCard("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RemoveManagedCard_NoApiKey_BadRequest(){
        ManagedCardsService.removeManagedCard("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void RemoveManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService.removeManagedCard(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void RemoveManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService.removeManagedCard(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RemoveManagedCard_UnknownManagedCardId_NotFound() {
        ManagedCardsService.removeManagedCard(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("RemoveManagedCard_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void RemoveManagedCard_NoManagedCardId_NotFound() {
        ManagedCardsService.removeManagedCard(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void RemoveManagedCard_CrossIdentityCheck_NotFound() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void RemoveManagedCard_InstrumentWithFunds_BalanceNotZero() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                .getManagedCardId();

        transferFundsToCard(consumerAuthenticationToken, CONSUMER,
                managedCardId, consumerCurrency, 150L, 1);

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BALANCE_NOT_ZERO"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void RemoveManagedCard_BackofficeCorporateImpersonator_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void RemoveManagedCard_BackofficeConsumerImpersonator_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, getBackofficeImpersonateToken(consumerId, CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void RemoveManagedCard_InstrumentLost_Success() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void RemoveManagedCard_InstrumentStolen_InstrumentAlreadyRemoved() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REMOVED"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}