package opc.junit.multi.managedcards;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class ReportLostCardTests extends BaseManagedCardsSetup {

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
    public void ReportLostCard_PrepaidCorporate_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_PrepaidConsumer_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_DebitCorporate_Success(){
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_DebitConsumer_Success(){
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_CorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, user.getRight())
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_CardNotUpgraded_InstrumentNotPhysical(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void ReportLostCard_CardUpgradedNotActivated_Success(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_CardAlreadyReported_InstrumentAlreadyMarkedLost(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_MARKED_LOST"));
    }

    @Test
    public void ReportLostCard_CardBlocked_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("USER"));
    }

    @Test
    public void ReportLostCard_CardDestroyed_InstrumentDestroyed(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void ReportLostCard_CardDamaged_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.replaceDamagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_CardStolen_InstrumentAlreadyMarkedStolen(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_MARKED_STOLEN"));
    }

    @Test
    public void ReportLostCard_CardReplacedAfterReportedLost__InstrumentAlreadyMarkedLost(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.replaceLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_MARKED_LOST"));
    }

    @Test
    public void ReportLostCard_CorporateReportUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, user.getRight())
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo("LOST"));
    }

    @Test
    public void ReportLostCard_UserReportCorporateCard_Unauthorized(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void ReportLostCard_UnknownManagedCardId_NotFound() {
        ManagedCardsService
                .reportLostCard(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReportLostCard_NoManagedCardId_NotFound() {
        ManagedCardsService
                .reportLostCard(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReportLostCard_CrossIdentityCheck_NotFound(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReportLostCard_InvalidApiKey_Unauthorised(){
        ManagedCardsService
                .reportLostCard("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReportLostCard_NoApiKey_Unauthorised(){
        ManagedCardsService
                .reportLostCard("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ReportLostCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService
                .reportLostCard(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReportLostCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService
                .reportLostCard(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReportLostCard_BackofficeCorporateImpersonator_Forbidden(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReportLostCard_BackofficeConsumerImpersonator_Forbidden(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportLostCard(secretKey, managedCardId, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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