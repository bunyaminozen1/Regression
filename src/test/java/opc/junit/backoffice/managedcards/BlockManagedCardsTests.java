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
import static opc.enums.opc.IdentityType.CORPORATE;
import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;

public class BlockManagedCardsTests extends BaseManagedCardsSetup{
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static String corporateImpersonateToken;
    private static String consumerImpersonateToken;

    @BeforeAll
    public static void IndividualSetup(){
        corporateSetup();
        consumerSetup();
       // corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);
        //consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);
        consumerImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(consumerId, IdentityType.CONSUMER, secretKey);
        corporateImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(corporateId, IdentityType.CORPORATE,secretKey);
    }

    @Test
    public void BlockManagedCard_PrepaidCorporate_Success() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedCard_PrepaidConsumer_Success() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedCard_DebitCorporate_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedCard_DebitConsumer_Success() {
        final String managedCardId =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedCard_PrepaidCardWithFunds_Success() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        transferFundsToCard(corporateAuthenticationToken, CORPORATE,
                managedCardId, corporateCurrency, 100L, 1);

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedCard_AlreadyBlocked_InstrumentAlreadyBlocked() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_BLOCKED"));
    }

    @Test
    public void BlockManagedCard_InvalidApiKey_Unauthorised(){
        BackofficeMultiService.blockManagedCard("abc", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void BlockManagedCard_NoApiKey_BadRequest(){
        BackofficeMultiService.blockManagedCard("", RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void BlockManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService.blockManagedCard(secretKey, RandomStringUtils.randomNumeric(18), consumerImpersonateToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void BlockManagedCard_RootUserLoggedOut_Forbidden(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        BackofficeMultiService.blockManagedCard(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void BlockManagedCard_UnknownManagedCardId_NotFound() {
        BackofficeMultiService.blockManagedCard(secretKey, RandomStringUtils.randomNumeric(18), corporateImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("BlockManagedCard_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void BlockManagedCard_NoManagedCardId_NotFound() {
        BackofficeMultiService.blockManagedCard(secretKey, "", corporateImpersonateToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void BlockManagedCard_CrossIdentityCheck_NotFound() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, consumerImpersonateToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void BlockManagedCard_InstrumentDestroyed_InstrumentDestroyed() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void BlockManagedCard_InstrumentLost_InstrumentMarkedLostStolen() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_MARKED_LOST_STOLEN"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"));
    }

    @Test
    public void BlockManagedCard_InstrumentStolen_InstrumentMarkedLostStolen() {
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken);

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateImpersonateToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_MARKED_LOST_STOLEN"));

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"));
    }

    @Test
    public void BlockManagedCard_CorporateAuthToken_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void BlockManagedCard_ConsumerAuthToken_Forbidden() {
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        BackofficeMultiService.blockManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
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
