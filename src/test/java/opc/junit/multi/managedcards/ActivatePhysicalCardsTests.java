package opc.junit.multi.managedcards;

import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedCardMode;
import opc.enums.opc.ManufacturingState;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.users.UsersModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class ActivatePhysicalCardsTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void IndividualSetup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void ActivateManagedCard_PrepaidCorporate_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE,
                0, false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_PrepaidConsumer_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken),
                managedCard,
                consumerPrepaidManagedCardsProfileId,
                IdentityType.CONSUMER,
                0, false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_DebitCorporate_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporateDebitManagedCardsProfileId,
                IdentityType.CORPORATE,
                0, false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_DebitConsumer_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken),
                managedCard,
                consumerDebitManagedCardsProfileId,
                IdentityType.CONSUMER,
                0, false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_CorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), user.getRight(), deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getRight()),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE,
                0, false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_CardWithFunds_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, deliveryAddress);

        final Long cardBalance = 20L;
        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, cardBalance, 1);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE,
                cardBalance.intValue(), false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_CardReplacementForANonActiveCardReportedLostOrStolen_Success(){
        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, deliveryAddress);

        ManagedCardsService.reportLostCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String replacementCardId =
                ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                        secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("physicalCardDetails.replacement.replacementId");

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, replacementCardId, corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE,
                0, true, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_CardReplacementAfterReportedLostOrStolen_Success(){
        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsService.reportLostCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String replacementCardId =
                ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),
                        secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("physicalCardDetails.replacement.replacementId");

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, replacementCardId, corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE,
                0, true, managedCard.getPhysicalCardAddressModel());
    }

    @Test
    public void ActivateManagedCard_CardNotUpgraded_InstrumentNotPhysical(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void ActivateManagedCard_CardAlreadyActive_InstrumentAlreadyActive(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_ACTIVATED"));
    }

    @Test
    public void ActivateManagedCard_CardBlocked_InstrumentBlocked(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_BLOCKED"));
    }

    @Test
    public void ActivateManagedCard_CardDestroyed_InstrumentDestroyed(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void ActivateManagedCard_CorporateActivateUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), user.getRight(), deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        assertSuccessfulResponse(ManagedCardsService
                        .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE,
                0, false, deliveryAddress);
    }

    @Test
    public void ActivateManagedCard_UserActivateCorporateCard_Unauthorised(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateManagedCard_InvalidCode_ActivationCodeInvalid(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel =
                new ActivatePhysicalCardModel(RandomStringUtils.randomNumeric(6));

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACTIVATION_CODE_INVALID"));
    }

    @Test
    public void ActivateManagedCard_UnknownManagedCardId_NotFound() {
        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateManagedCard_NoManagedCardId_NotFound() {
        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateManagedCard_CrossIdentityCheck_NotFound(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateManagedCard_InvalidApiKey_Unauthorised(){
        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, "abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateManagedCard_NoApiKey_BadRequest(){
        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, "", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ActivateManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateManagedCard_BackofficeCorporateImpersonator_Forbidden(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateManagedCard_BackofficeConsumerImpersonator_Forbidden(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken, deliveryAddress);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
                .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void assertSuccessfulResponse(final Response response,
                                          final ManagedCardDetails managedCardDetails,
                                          final String managedCardProfileId,
                                          final IdentityType identityType,
                                          final int balance,
                                          final boolean isReplacement,
                                          final PhysicalCardAddressModel physicalCardAddressModel){
        response
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(managedCardProfileId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(managedCardDetails.getManagedCardModel().getTag()))
                .body("friendlyName", equalTo(isReplacement ? String.format("%s Replacement", managedCardDetails.getManagedCardModel().getFriendlyName())
                        : managedCardDetails.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo("ACTIVE"))
                .body("type", equalTo("PHYSICAL"))
                .body("cardBrand", equalTo("MASTERCARD"))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", notNullValue())
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(managedCardDetails.getManagedCardModel().getNameOnCard()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("cardLevelClassification", equalTo(identityType.toString()))
                .body("expiryPeriodMonths", equalTo(36))
                .body("renewalType", equalTo("NO_RENEW"))
                .body("creationTimestamp", notNullValue())
                .body("cardholderMobileNumber", equalTo(managedCardDetails.getManagedCardModel().getCardholderMobileNumber()))
                .body("billingAddress.addressLine1", equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getAddressLine1()))
                .body("billingAddress.addressLine2", equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getAddressLine2()))
                .body("billingAddress.city", equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getCity()))
                .body("billingAddress.postCode", equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getPostCode()))
                .body("billingAddress.state", equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getState()))
                .body("billingAddress.country", equalTo(managedCardDetails.getManagedCardModel().getBillingAddress().getCountry()))
                .body("physicalCardDetails.productReference", notNullValue())
                .body("physicalCardDetails.carrierType", notNullValue())
                .body("physicalCardDetails.pendingActivation", equalTo(false))
                .body("physicalCardDetails.pinBlocked", equalTo(false))
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                .body("physicalCardDetails.deliveryAddress.name", equalTo(physicalCardAddressModel.getName()))
                .body("physicalCardDetails.deliveryAddress.surname", equalTo(physicalCardAddressModel.getSurname()))
                .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(physicalCardAddressModel.getAddressLine1()))
                .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(physicalCardAddressModel.getAddressLine2()))
                .body("physicalCardDetails.deliveryAddress.city", equalTo(physicalCardAddressModel.getCity()))
                .body("physicalCardDetails.deliveryAddress.postCode", equalTo(physicalCardAddressModel.getPostCode()))
                .body("physicalCardDetails.deliveryAddress.state", equalTo(physicalCardAddressModel.getState()))
                .body("physicalCardDetails.deliveryAddress.country", equalTo(physicalCardAddressModel.getCountry()))
                .body("physicalCardDetails.deliveryMethod", equalTo("STANDARD_DELIVERY"))
                .body("physicalCardDetails.manufacturingState", equalTo(ManufacturingState.DELIVERED.name()))
                .body("mode", equalTo(managedCardDetails.getManagedCardModel().getMode()));

        if (managedCardDetails.getManagedCardMode() == ManagedCardMode.PREPAID_MODE) {
            response.then()
                    .body("currency", equalTo(managedCardDetails.getManagedCardModel().getCurrency()))
                    .body("balances.availableBalance", equalTo(balance))
                    .body("balances.actualBalance", equalTo(balance));
        } else {
            response.then()
                    .body("parentManagedAccountId", equalTo(managedCardDetails.getManagedCardModel().getParentManagedAccountId()));
        }
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