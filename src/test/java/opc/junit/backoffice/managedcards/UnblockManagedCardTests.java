package opc.junit.backoffice.managedcards;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;

public class UnblockManagedCardTests extends BaseManagedCardsSetup{

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
        corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
    }

    @Test
    public void UnblockManagedCard_PrepaidCorporate_Success() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedCard_PrepaidConsumer_Success() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedCard_DebitCorporate_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedCard_DebitConsumer_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void UnblockManagedCard_AlreadyUnblocked_InstrumentNotBlocked() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.unblockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_BLOCKED"));
    }

    @Test
    public void UnblockManagedCard_InActiveState_InstrumentNotBlocked() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_BLOCKED"));
    }

    @Test
    public void UnblockManagedCard_InvalidApiKey_Unauthorised(){
        BackofficeMultiService.unblockManagedCard("abc", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UnblockManagedCard_NoApiKey_Unauthorised(){
        BackofficeMultiService.unblockManagedCard("", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void UnblockManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService.unblockManagedCard(secretKey, RandomStringUtils.randomNumeric(18), consumerImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UnblockManagedCard_RootUserLoggedOut_Forbidden(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        BackofficeMultiService.unblockManagedCard(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UnblockManagedCard_UnknownManagedCardId_NotFound() {
        BackofficeMultiService.unblockManagedCard(secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("UnblockManagedCard_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void UnblockManagedCard_NoManagedCardId_NotFound() {
        BackofficeMultiService.unblockManagedCard(secretKey, "", corporateImpersonateToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void UnblockManagedCard_CrossIdentityCheck_NotFound() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void UnlockManagedCard_InstrumentDestroyed_InstrumentDestroyed() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void UnblockManagedCard_CorporateAuthToken_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UnblockManagedCard_ConsumerAuthToken_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UnblockManagedCard_InstrumentLost_InstrumentMarkedLostStolen() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_MARKED_LOST_STOLEN"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void UnblockManagedCard_InstrumentStolen_InstrumentMarkedLostStolen() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken);

        BackofficeMultiService.unblockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_MARKED_LOST_STOLEN"));

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
