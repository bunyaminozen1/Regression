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
public class ReportStolenCardTests extends BaseManagedCardsSetup {

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
    public void ReportStolenCard_PrepaidCorporate_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_PrepaidConsumer_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_DebitCorporate_Success(){
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_DebitConsumer_Success(){
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_CorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, user.getRight())
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_CardNotUpgraded_InstrumentNotPhysical(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void ReportStolenCard_CardUpgradedNotActivated_PhysicalCardNotActivated(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCardId, consumerAuthenticationToken);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PHYSICAL_CARD_NOT_ACTIVATED"));
    }

    @Test
    public void ReportStolenCard_CardAlreadyReported_InstrumentAlreadyMarkedStolen(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_MARKED_STOLEN"));
    }

    @Test
    public void ReportStolenCard_CardBlocked_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_CardDestroyed_InstrumentAlreadyDestroyed(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void ReportStolenCard_CardDamaged_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.replaceDamagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_CardLost_InstrumentAlreadyMarkedLost(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_MARKED_LOST"));
    }

    @Test
    public void ReportStolenCard_CardReplacedAfterReportedLost_InstrumentAlreadyMarkedLost(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.replaceLostCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_MARKED_LOST"));
    }

    @Test
    public void ReportStolenCard_CorporateReportUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, user.getRight())
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("DESTROYED"))
                .body("state.destroyedReason", equalTo("STOLEN"));
    }

    @Test
    public void ReportStolenCard_UserReportCorporateCard_Unauthorized(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);

        ManagedCardsService
                .getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_OK)
                .body("state.state", equalTo("ACTIVE"));
    }

    @Test
    public void ReportStolenCard_UnknownManagedCardId_NotFound() {
        ManagedCardsService
                .reportStolenCard(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReportStolenCard_NoManagedCardId_NotFound() {
        ManagedCardsService
                .reportStolenCard(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReportStolenCard_CrossIdentityCheck_NotFound(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ReportStolenCard_InvalidApiKey_Unauthorised(){
        ManagedCardsService
                .reportStolenCard("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReportStolenCard_NoApiKey_BadRequest(){
        ManagedCardsService
                .reportStolenCard("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ReportStolenCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService
                .reportStolenCard(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReportStolenCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService
                .reportStolenCard(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ReportStolenCard_BackofficeCorporateImpersonator_Forbidden(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ReportStolenCard_BackofficeConsumerImpersonator_Forbidden(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .reportStolenCard(secretKey, managedCardId, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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