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
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class GetPhysicalCardPinTests extends BaseManagedCardsSetup {

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
    public void GetPhysicalCardPin_PrepaidCorporate_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("pin.value", notNullValue());
    }

    @Test
    public void GetPhysicalCardPin_PrepaidConsumer_Success(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("pin.value", notNullValue());
    }

    @Test
    public void GetPhysicalCardPin_DebitCorporate_Success(){
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("pin.value", notNullValue());
    }

    @Test
    public void GetPhysicalCardPin_DebitConsumer_Success(){
        final String managedCardId =
                createManagedAccountAndPhysicalDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("pin.value", notNullValue());
    }

    @Test
    public void GetPhysicalCardPin_PrepaidCorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("pin.value", notNullValue());
    }

    @Test
    public void GetPhysicalCardPin_CardNotActive_Conflict(){
        ManagedCardsService
                .getPhysicalCardPin(secretKey, createPhysicalNonActivatedManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                    .getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PHYSICAL_CARD_NOT_ACTIVATED"));
    }

    @Test
    public void GetPhysicalCardPin_CardNotUpgraded_InstrumentNotPhysical(){
        final String managedCardId =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void GetPhysicalCardPin_CardBlocked_InstrumentBlocked(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_BLOCKED"));
    }

    @Test
    public void GetPhysicalCardPin_CardDestroyed_InstrumentDestroyed(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void GetPhysicalCardPin_CorporateGetUserCardPin_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight())
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("pin.value", notNullValue());
    }

    @Test
    public void GetPhysicalCardPin_UserGetCorporateCardPin_Unauthorized(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPhysicalCardPin_UnknownManagedCardId_NotFound() {
        ManagedCardsService
                .getPhysicalCardPin(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPhysicalCardPin_NoManagedCardId_NotFound() {
        ManagedCardsService
                .getPhysicalCardPin(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPhysicalCardPin_CrossIdentityCheck_NotFound(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetPhysicalCardPin_InvalidApiKey_Unauthorised(){
        ManagedCardsService
                .getPhysicalCardPin("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPhysicalCardPin_NoApiKey_BadRequest(){
        ManagedCardsService
                .getPhysicalCardPin("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetPhysicalCardPin_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService
                .getPhysicalCardPin(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPhysicalCardPin_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPhysicalCardPin_BackofficeCorporateImpersonator_Forbidden(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetPhysicalCardPin_BackofficeConsumerImpersonator_Forbidden(){
        final String managedCardId =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken)
                        .getManagedCardId();

        ManagedCardsService
                .getPhysicalCardPin(secretKey, managedCardId, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
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

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}