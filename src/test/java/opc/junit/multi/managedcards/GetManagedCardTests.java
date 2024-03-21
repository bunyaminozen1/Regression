package opc.junit.multi.managedcards;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static opc.enums.opc.CardLevelClassification.CONSUMER;
import static opc.enums.opc.CardLevelClassification.CORPORATE;
import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class GetManagedCardTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static ManagedCardDetails corporatePrepaidManagedCard;
    private static ManagedCardDetails consumerPrepaidManagedCard;
    private static ManagedCardDetails corporateDebitManagedCard;
    private static ManagedCardDetails consumerDebitManagedCard;
    private static PhysicalCardAddressModel corporateDeliveryAddress;
    private static PhysicalCardAddressModel consumerDeliveryAddress;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateDeliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        consumerDeliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        corporatePrepaidManagedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        consumerPrepaidManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateDebitManagedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        consumerDebitManagedCard =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_PrepaidCorporate_Success(final InstrumentType instrumentType){

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                    corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final ValidatableResponse response =
                ManagedCardsService.getManagedCard(secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(corporateCurrency))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertSuccessfulResponse(response, corporatePrepaidManagedCard, corporatePrepaidManagedCardsProfileId,
                CORPORATE, instrumentType, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL), corporateCurrency);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_DebitCorporate_Success(final InstrumentType instrumentType){

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporateDebitManagedCard.getManagedCardId(),
                    corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final ValidatableResponse response =
                ManagedCardsService.getManagedCard(secretKey, corporateDebitManagedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(corporateDebitManagedCard.getManagedCardModel().getParentManagedAccountId()));

        assertSuccessfulResponse(response, corporateDebitManagedCard, corporateDebitManagedCardsProfileId,
                CORPORATE, instrumentType, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL), corporateCurrency);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_Consumer_Success(final InstrumentType instrumentType){

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, consumerPrepaidManagedCard.getManagedCardId(),
                    consumerAuthenticationToken, consumerDeliveryAddress);
        }

        final ValidatableResponse response =
                ManagedCardsService.getManagedCard(secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(consumerCurrency))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertSuccessfulResponse(response, consumerPrepaidManagedCard, consumerPrepaidManagedCardsProfileId,
                CONSUMER, instrumentType, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL), consumerCurrency);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void GetManagedCard_DebitConsumer_Success(final InstrumentType instrumentType){

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, consumerDebitManagedCard.getManagedCardId(),
                    consumerAuthenticationToken, consumerDeliveryAddress);
        }

        final ValidatableResponse response =
                ManagedCardsService.getManagedCard(secretKey, consumerDebitManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(consumerDebitManagedCard.getManagedCardModel().getParentManagedAccountId()));

        assertSuccessfulResponse(response, consumerDebitManagedCard, consumerDebitManagedCardsProfileId,
                CONSUMER, instrumentType, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL), consumerCurrency);
    }

    @Test
    public void GetManagedCard_CardUpgradedNotActivated_Success(){

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, consumerDebitManagedCard.getManagedCardId(),
                consumerAuthenticationToken, consumerDeliveryAddress);

        final ValidatableResponse response =
                ManagedCardsService.getManagedCard(secretKey, consumerDebitManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(consumerDebitManagedCard.getManagedCardModel().getParentManagedAccountId()));

        assertSuccessfulResponse(response, consumerDebitManagedCard, consumerDebitManagedCardsProfileId,
                CONSUMER, VIRTUAL, ManufacturingState.REQUESTED, true, consumerCurrency);
    }

    @Test
    public void GetManagedCard_UnknownManagedCardId_NotFound(){

        ManagedCardsService.getManagedCard(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedCard_InvalidApiKey_Unauthorised(){
        ManagedCardsService.getManagedCard("abc", consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCard_NoApiKey_BadRequest(){
        ManagedCardsService.getManagedCard("", consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedCardsService.getManagedCard(secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        ManagedCardsService.getManagedCard(secretKey, corporatePrepaidManagedCard.getManagedCardId(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedCard_CrossIdentityChecks_NotFound(){

        ManagedCardsService.getManagedCard(secretKey, corporatePrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedCard_BackofficeCorporateImpersonator_Forbidden(){

        ManagedCardsService.getManagedCard(secretKey, corporatePrepaidManagedCard.getManagedCardId(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedCard_BackofficeConsumerImpersonator_Forbidden(){

        ManagedCardsService.getManagedCard(secretKey, consumerPrepaidManagedCard.getManagedCardId(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest
    @EnumSource(BlockType.class)
    public void GetManagedCard_CardBlockedByAdmin_Success(final BlockType  blockType){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        AdminHelper.blockManagedCard(managedCard.getManagedCardId(), blockType, AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()));

        ManagedCardsService.getManagedCard(secretKey, managedCard.getManagedCardId(),
                        authenticatedCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state.state", equalTo("BLOCKED"))
                .body("state.blockedReason", equalTo(blockType.equals(BlockType.USER) ? "USER" : "SYSTEM"));
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final ManagedCardDetails managedCardDetails,
                                          final String managedCardsProfileId,
                                          final CardLevelClassification cardLevelClassification,
                                          final InstrumentType instrumentType,
                                          final ManufacturingState manufacturingState,
                                          final boolean isPhysical,
                                          final String currency){
        response.body("id", equalTo(managedCardDetails.getManagedCardId()))
                .body("profileId", equalTo(managedCardsProfileId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(managedCardDetails.getManagedCardModel().getTag()))
                .body("friendlyName", equalTo(managedCardDetails.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo(State.ACTIVE.name()))
                .body("type", equalTo(instrumentType.name()))
                .body("cardBrand", equalTo(CardBrand.MASTERCARD.name()))
                .body("cardLevelClassification", equalTo(cardLevelClassification.name()))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", notNullValue())
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(managedCardDetails.getManagedCardModel().getNameOnCard()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
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
                .body("digitalWallets.walletsEnabled", equalTo(false))
                .body("digitalWallets.artworkReference", equalTo(null));

        if(isPhysical){
            assertPhysicalCardDetails(response,
                    cardLevelClassification.equals(CORPORATE) ? corporateDeliveryAddress : consumerDeliveryAddress, manufacturingState);
        }

        if(managedCardDetails.getManagedCardMode() == DEBIT_MODE){
            response.body("availableToSpend[0].interval", equalTo("ALWAYS"))
                    .body("availableToSpend[0].value.amount", equalTo(0))
                    .body("availableToSpend[0].value.currency", equalTo(currency));
        }
    }

    private void assertPhysicalCardDetails(final ValidatableResponse response,
                                           final PhysicalCardAddressModel physicalCardAddressModel,
                                           final ManufacturingState manufacturingState){
        response
                .body("physicalCardDetails.productReference", notNullValue())
                .body("physicalCardDetails.carrierType", notNullValue())
                .body("physicalCardDetails.pendingActivation", equalTo(manufacturingState.equals(ManufacturingState.REQUESTED)))
                .body("physicalCardDetails.pinBlocked", equalTo(false))
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                .body("physicalCardDetails.deliveryMethod", equalTo("STANDARD_DELIVERY"))
                .body("physicalCardDetails.deliveryAddress.name", equalTo(physicalCardAddressModel.getName()))
                .body("physicalCardDetails.deliveryAddress.surname", equalTo(physicalCardAddressModel.getSurname()))
                .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(physicalCardAddressModel.getAddressLine1()))
                .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(physicalCardAddressModel.getAddressLine2()))
                .body("physicalCardDetails.deliveryAddress.city", equalTo(physicalCardAddressModel.getCity()))
                .body("physicalCardDetails.deliveryAddress.postCode", equalTo(physicalCardAddressModel.getPostCode()))
                .body("physicalCardDetails.deliveryAddress.state", equalTo(physicalCardAddressModel.getState()))
                .body("physicalCardDetails.deliveryAddress.country", equalTo(physicalCardAddressModel.getCountry()))
                .body("physicalCardDetails.manufacturingState", equalTo(manufacturingState.name()));
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