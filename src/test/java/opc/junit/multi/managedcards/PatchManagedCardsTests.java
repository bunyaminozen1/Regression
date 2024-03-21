package opc.junit.multi.managedcards;

import commons.enums.Currency;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.Comparator;
import opc.enums.opc.CardBrand;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.DeliveryMethod;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.ManufacturingState;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.ExternalDataModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovatornew.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class PatchManagedCardsTests extends BaseManagedCardsSetup {

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
    public void PatchManagedCard_PrepaidCorporate_Success(final InstrumentType instrumentType) {

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                    corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(corporatePrepaidManagedCardsProfileId))
                        .body("currency", equalTo(corporateCurrency))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonDetails(response, corporatePrepaidManagedCard, patchManagedCardModel, instrumentType,
                DeliveryMethod.STANDARD_DELIVERY, corporateDeliveryAddress, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_PrepaidConsumer_Success(final InstrumentType instrumentType) {

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, consumerPrepaidManagedCard.getManagedCardId(),
                    consumerAuthenticationToken, consumerDeliveryAddress);
        }

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(consumerPrepaidManagedCardsProfileId))
                        .body("currency", equalTo(consumerCurrency))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CONSUMER.name()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonDetails(response, consumerPrepaidManagedCard, patchManagedCardModel, instrumentType,
                DeliveryMethod.STANDARD_DELIVERY, consumerDeliveryAddress, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_DebitCorporate_Success(final InstrumentType instrumentType) {

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporateDebitManagedCard.getManagedCardId(),
                    corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporateDebitManagedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(corporateDebitManagedCardsProfileId))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                        .body("parentManagedAccountId", equalTo(corporateDebitManagedCard.getManagedCardModel()
                                .getParentManagedAccountId()));

        assertCommonDetails(response, corporateDebitManagedCard, patchManagedCardModel, instrumentType,
                DeliveryMethod.STANDARD_DELIVERY, corporateDeliveryAddress, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_DebitConsumer_Success(final InstrumentType instrumentType) {

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, consumerDebitManagedCard.getManagedCardId(),
                    consumerAuthenticationToken, consumerDeliveryAddress);
        }

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerDebitManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(consumerDebitManagedCardsProfileId))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CONSUMER.name()))
                        .body("parentManagedAccountId", equalTo(consumerDebitManagedCard.getManagedCardModel()
                                .getParentManagedAccountId()));

        assertCommonDetails(response, consumerDebitManagedCard, patchManagedCardModel, instrumentType,
                DeliveryMethod.STANDARD_DELIVERY, consumerDeliveryAddress, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL));
    }

    @Test
    public void PatchManagedCard_CardUpgradedNotActivated_Success() {

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(corporatePrepaidManagedCardsProfileId))
                        .body("currency", equalTo(corporateCurrency))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonDetails(response, corporatePrepaidManagedCard, patchManagedCardModel, VIRTUAL,
                DeliveryMethod.STANDARD_DELIVERY, corporateDeliveryAddress, ManufacturingState.REQUESTED, true);
    }

    @Test
    public void PatchManagedCard_PatchDeliveryAddressAndMethod_Success() {

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDeliveryAddress(deliveryAddress)
                        .setDeliveryMethod(DeliveryMethod.REGISTERED_MAIL.name())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(corporatePrepaidManagedCardsProfileId))
                        .body("currency", equalTo(corporateCurrency))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonDetails(response, managedCard, patchManagedCardModel, PHYSICAL,
                DeliveryMethod.REGISTERED_MAIL, deliveryAddress, ManufacturingState.DELIVERED, true);
    }

    @Test
    public void PatchManagedCard_SameCall_Success() {

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(),
                consumerAuthenticationToken));
        responses.add(ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(),
                consumerAuthenticationToken));

        responses.forEach(response -> {
            response.then()
                    .statusCode(SC_OK);

            assertCommonDetails(response.then(), managedCard, patchManagedCardModel, VIRTUAL,
                    DeliveryMethod.STANDARD_DELIVERY, consumerDeliveryAddress, ManufacturingState.DELIVERED, false);
        });
    }

    @Test
    public void PatchManagedCard_PatchDeliveryAddressMissingDetails_BadRequest() {

        final ManagedCardDetails managedCard =
                createPhysicalPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel()
                .setName(null)
                .setSurname(null)
                .build();

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDeliveryAddress(deliveryAddress)
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCard_PatchDeliveryAddressCardNotPhysical_InstrumentNotPhysical() {

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel deliveryAddress = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDeliveryAddress(deliveryAddress)
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_PHYSICAL"));
    }

    @Test
    public void PatchManagedCard_NameOnCardTooLong_Success() {
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setNameOnCard(String.format("%s", RandomStringUtils.randomAlphabetic(30)))
                .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.nameOnCard: size must be between 0 and 27"));
    }

    @Test
    public void PatchManagedCard_NameOnCardLine2TooLong_BadRequest() {
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setNameOnCardLine2(String.format("%s", RandomStringUtils.randomAlphabetic(30)))
                .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.nameOnCardLine2: size must be between 0 and 27"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void PatchManagedCard_NoNameOnCard_Success(final String nameOnCard) {
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setNameOnCard(nameOnCard)
                .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
            .then()
            .statusCode(SC_OK)
            .body("nameOnCard", equalTo(managedCard.getManagedCardModel().getNameOnCard()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void PatchManagedCard_NoNameOnCardLine2_Success(final String nameOnCardLine2) {
        final CreateManagedCardModel createManagedCardModel =
            CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId,
                    consumerCurrency)
                .setNameOnCardLine2(String.format("%s", RandomStringUtils.randomAlphabetic(5)))
                .build();

        final String managedCardId =
            ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);


        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setNameOnCardLine2(nameOnCardLine2)
                .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, consumerAuthenticationToken)
            .then()
            .statusCode(SC_OK)
            .body("nameOnCardLine2", equalTo(createManagedCardModel.getNameOnCardLine2()));
    }

    @ParameterizedTest
    @EmptySource
    public void PatchManagedCard_EmptyFriendlyNameValue_BadRequest(final String friendlyName) {
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setFriendlyName(friendlyName)
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @EmptySource
    public void PatchManagedCard_EmptyMobileNumberValue_BadRequest(final String mobileNumber) {
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setCardholderMobileNumber(mobileNumber)
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCard_InvalidApiKey_Unauthorised(){
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, "abc", consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCard_NoApiKey_BadRequest(){
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, "", consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchManagedCard_UnknownManagedCardId_NotFound() {
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCard_CrossIdentityManagedCardId_NotFound() {
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchManagedCard_NoManagedCardId_MethodNotAllowed() {
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void PatchManagedCard_BackofficeCorporateImpersonator_Forbidden() {

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchManagedCard_BackofficeConsumerImpersonator_Forbidden() {

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(),
                getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @MethodSource("getDefaultTimeoutDecision")
    public void PatchManagedCard_DebitCorporate_AuthForwardingDefaultTimeoutDecision_Approve_Success(DefaultTimeoutDecision defaultTimeoutDecision) {

        authForwardingConfiguration(true, true);

        final String managedCardId = createCorporateDebitAuthForwardingEnabledManagedCard(true, DefaultTimeoutDecision.APPROVE);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardAuthForwardingModel(defaultTimeoutDecision.name()).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("authForwardingDefaultTimeoutDecision", equalTo(defaultTimeoutDecision.name()));
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @MethodSource("getDefaultTimeoutDecision")
    public void PatchManagedCard_PrepaidCorporate_AuthForwardingDefaultTimeoutDecision_Approve_Success(DefaultTimeoutDecision defaultTimeoutDecision) {

        authForwardingConfiguration(true, true);

        final String managedCardId = createCorporatePrepaidAuthForwardingEnabledManagedCard(true, DefaultTimeoutDecision.APPROVE);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardAuthForwardingModel(defaultTimeoutDecision.name()).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("authForwardingDefaultTimeoutDecision", equalTo(defaultTimeoutDecision.name()));
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @MethodSource("authForwardingLevels")
    public void PatchManagedCard_DebitCorporate_AuthForwardingDefaultTimeoutDecision__LevelsDisabled_Conflict(final boolean enableInnovator,
                                                                                                             final boolean enableProgramme,
                                                                                                             final boolean enableCardProfile) {
        if (enableCardProfile)
            authForwardingConfiguration(true, true);

        final String managedCardId = createCorporateDebitAuthForwardingEnabledManagedCard(enableCardProfile, DefaultTimeoutDecision.APPROVE);

        authForwardingConfiguration(enableInnovator, enableProgramme);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardAuthForwardingModel(DefaultTimeoutDecision.APPROVE.name()).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AUTH_FORWARDING_NOT_ENABLED"));
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @MethodSource("authForwardingLevels")
    public void PatchManagedCard_PrepaidCorporate_AuthForwardingDefaultTimeoutDecision__LevelsDisabled_Conflict(final boolean enableInnovator,
                                                                                                               final boolean enableProgramme,
                                                                                                               final boolean enableCardProfile) {
        if (enableCardProfile)
            authForwardingConfiguration(true, true);

        final String managedCardId = createCorporatePrepaidAuthForwardingEnabledManagedCard(enableCardProfile, DefaultTimeoutDecision.APPROVE);

        authForwardingConfiguration(enableInnovator, enableProgramme);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardAuthForwardingModel(DefaultTimeoutDecision.APPROVE.name()).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AUTH_FORWARDING_NOT_ENABLED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_ExternalData_Success(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final String externalDataName = RandomStringUtils.randomAlphabetic(10);
        final String externalDataValue = RandomStringUtils.randomAlphabetic(10);

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        final ValidatableResponse response =
            ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("profileId", equalTo(corporatePrepaidManagedCardsProfileId))
                .body("currency", equalTo(corporateCurrency))
                .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("externalData[0].name", equalTo(externalDataName))
                .body("externalData[0].value", equalTo(externalDataValue));

        assertCommonDetails(response, corporatePrepaidManagedCard, patchManagedCardModel, instrumentType,
            DeliveryMethod.STANDARD_DELIVERY, corporateDeliveryAddress, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_MultipleExternalData_Success(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
        createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        List<ExternalDataModel> externalData = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ExternalDataModel element = ExternalDataModel.builder()
                .name(RandomStringUtils.randomAlphabetic(10))
                .value(RandomStringUtils.randomAlphabetic(10))
                .build();
            externalData.add(element);
        }

        externalData.sort(Comparator.comparing(ExternalDataModel::getName));

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        final ValidatableResponse response =
            ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("profileId", equalTo(corporatePrepaidManagedCardsProfileId))
                .body("currency", equalTo(corporateCurrency))
                .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));

        for (ExternalDataModel element : externalData) {
            assertThat(response.extract().jsonPath().getList("externalData.name"), hasItem(element.getName()));
            assertThat(response.extract().jsonPath().getList("externalData.value"), hasItem(element.getValue()));
        }

        assertCommonDetails(response, corporatePrepaidManagedCard, patchManagedCardModel, instrumentType,
            DeliveryMethod.STANDARD_DELIVERY, corporateDeliveryAddress, ManufacturingState.DELIVERED, instrumentType.equals(PHYSICAL));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_ExternalDataNameTooLong_BadRequest(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final String externalDataName = RandomStringUtils.randomAlphabetic(51);
        final String externalDataValue = RandomStringUtils.randomAlphabetic(10);

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.externalData[0].name: size must be between 1 and 50"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_ExternalDataValueTooLong_BadRequest(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final String externalDataName = RandomStringUtils.randomAlphabetic(10);
        final String externalDataValue = RandomStringUtils.randomAlphabetic(51);

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.externalData[0].value: size must be between 1 and 50"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_ExternalDataTooManyElements_BadRequest(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        List<ExternalDataModel> externalData = new ArrayList<>();

        for (int i = 0; i < 11; i++) {
            ExternalDataModel element = ExternalDataModel.builder()
                .name(RandomStringUtils.randomAlphabetic(10))
                .value(RandomStringUtils.randomAlphabetic(10))
                .build();
            externalData.add(element);
        }

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.externalData: size must be between 0 and 10"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_ExternalDataInvalidCharacters_BadRequest(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final String externalDataName = "!.#$";
        final String externalDataValue = "!.#$";

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("_embedded.errors[0].message", equalTo("request.externalData[0].name: must match \"^[a-zA-Z0-9 ]+$\""))
            .body("_embedded.errors[1].message", equalTo("request.externalData[0].value: must match \"^[a-zA-Z0-9 ]+$\""));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_ExternalDataEmptyString_BadRequest(final InstrumentType instrumentType) {

        final ManagedCardDetails corporatePrepaidManagedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name("").value("").build());

        final PatchManagedCardModel patchManagedCardModel =
            PatchManagedCardModel.DefaultPatchManagedCardModel()
                .setExternalData(externalData).build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.externalData[0].name: must match \"^[a-zA-Z0-9 ]+$\""))
                .body("_embedded.errors[1].message", equalTo("request.externalData[0].name: size must be between 1 and 50"))
                .body("_embedded.errors[2].message", equalTo("request.externalData[0].value: must match \"^[a-zA-Z0-9 ]+$\""))
                .body("_embedded.errors[3].message", equalTo("request.externalData[0].value: size must be between 1 and 50"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void PatchManagedCard_PrepaidRenewalType_Success(final InstrumentType instrumentType) {

        if (instrumentType.equals(PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, corporatePrepaidManagedCard.getManagedCardId(),
                corporateAuthenticationToken, corporateDeliveryAddress);
        }

        final PatchManagedCardModel patchManagedCardModelRenew =
            PatchManagedCardModel.builder().setRenewalType("RENEW").build();;

        ManagedCardsService.patchManagedCard(patchManagedCardModelRenew, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_OK)
            .body("renewalTimestamp", notNullValue())
            .body("renewalType", equalTo("RENEW"));

        final PatchManagedCardModel patchManagedCardModelNoRenew =
            PatchManagedCardModel.builder().setRenewalType("NO_RENEW").build();;

        ManagedCardsService.patchManagedCard(patchManagedCardModelNoRenew, secretKey, corporatePrepaidManagedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_OK)
            .body("renewalType", equalTo("NO_RENEW"))
            .body("renewalTimestamp", nullValue());
    }

    private void assertCommonDetails(final ValidatableResponse response,
                                     final ManagedCardDetails managedCard,
                                     final PatchManagedCardModel patchManagedCardModel,
                                     final InstrumentType instrumentType,
                                     final DeliveryMethod deliveryMethod,
                                     final PhysicalCardAddressModel physicalCardAddressModel,
                                     final ManufacturingState manufacturingState,
                                     final boolean isPhysical) {

        response.body("id", equalTo(managedCard.getManagedCardId()))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(patchManagedCardModel.getTag() == null ?
                        managedCard.getManagedCardModel().getTag() : patchManagedCardModel.getTag()))
                .body("friendlyName", equalTo(patchManagedCardModel.getFriendlyName() == null ?
                        managedCard.getManagedCardModel().getFriendlyName() : patchManagedCardModel.getFriendlyName()))
                .body("state.state", equalTo(State.ACTIVE.name()))
                .body("type", equalTo(instrumentType.name()))
                .body("cardBrand", equalTo(CardBrand.MASTERCARD.name()))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", notNullValue())
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(patchManagedCardModel.getNameOnCard()))
                .body("nameOnCardLine2", equalTo(patchManagedCardModel.getNameOnCardLine2()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryPeriodMonths", equalTo(36))
                .body("renewalType", equalTo("NO_RENEW"))
                .body("creationTimestamp", notNullValue())
                .body("cardholderMobileNumber", equalTo(patchManagedCardModel.getCardholderMobileNumber() == null ? managedCard.getManagedCardModel().getCardholderMobileNumber() : patchManagedCardModel.getCardholderMobileNumber()))
                .body("mode", equalTo(managedCard.getManagedCardModel().getMode()));

        assertBillingAddress(response, managedCard, patchManagedCardModel);

        if(isPhysical){
            assertPhysicalCardDetails(response, deliveryMethod, physicalCardAddressModel, manufacturingState);
        }
    }

    private void assertBillingAddress(final ValidatableResponse response,
                                      final ManagedCardDetails managedCard,
                                      final PatchManagedCardModel patchManagedCardModel) {
        if (patchManagedCardModel.getBillingAddress() == null) {
            response
                    .body("billingAddress.addressLine1", equalTo(managedCard.getManagedCardModel().getBillingAddress().getAddressLine1()))
                    .body("billingAddress.addressLine2", equalTo(managedCard.getManagedCardModel().getBillingAddress().getAddressLine2()))
                    .body("billingAddress.city", equalTo(managedCard.getManagedCardModel().getBillingAddress().getCity()))
                    .body("billingAddress.postCode", equalTo(managedCard.getManagedCardModel().getBillingAddress().getPostCode()))
                    .body("billingAddress.state", equalTo(managedCard.getManagedCardModel().getBillingAddress().getState()))
                    .body("billingAddress.country", equalTo(managedCard.getManagedCardModel().getBillingAddress().getCountry()));
        } else {
            response
                    .body("billingAddress.addressLine1", equalTo(patchManagedCardModel.getBillingAddress().getAddressLine1()))
                    .body("billingAddress.addressLine2", equalTo(patchManagedCardModel.getBillingAddress().getAddressLine2()))
                    .body("billingAddress.city", equalTo(patchManagedCardModel.getBillingAddress().getCity()))
                    .body("billingAddress.postCode", equalTo(patchManagedCardModel.getBillingAddress().getPostCode()))
                    .body("billingAddress.state", equalTo(patchManagedCardModel.getBillingAddress().getState()))
                    .body("billingAddress.country", equalTo(patchManagedCardModel.getBillingAddress().getCountry()));
        }
    }

    private void assertPhysicalCardDetails(final ValidatableResponse response,
                                           final DeliveryMethod deliveryMethod,
                                           final PhysicalCardAddressModel physicalCardAddressModel,
                                           final ManufacturingState manufacturingState){
        response
                .body("physicalCardDetails.productReference", notNullValue())
                .body("physicalCardDetails.carrierType", notNullValue())
                .body("physicalCardDetails.pendingActivation", equalTo(manufacturingState.equals(ManufacturingState.REQUESTED)))
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
                .body("physicalCardDetails.deliveryMethod", equalTo(deliveryMethod.name()))
                .body("physicalCardDetails.manufacturingState", equalTo(manufacturingState.name()));
    }

    protected static String createCorporateDebitAuthForwardingEnabledManagedCard(final boolean authForwardingEnabled, final DefaultTimeoutDecision defaultTimeoutDecision) {
        final String managedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, Currency.GBP.name(), corporateAuthenticationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getCorporateDebitAuthForwardingCardProfileModel(authForwardingEnabled, defaultTimeoutDecision.name());

        final String profileId = InnovatorService.createManagedCardsProfileV2(createManagedCardsProfile, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId)
                .setProfileId(profileId)
                .build();

       return ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);
    }

    protected static String createCorporatePrepaidAuthForwardingEnabledManagedCard(final boolean authForwardingEnabled, final DefaultTimeoutDecision defaultTimeoutDecision) {

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getCorporatePrepaidAuthForwardingCardProfileModel(authForwardingEnabled, defaultTimeoutDecision.name());

        final String profileId = InnovatorService.createManagedCardsProfileV2(createManagedCardsProfile, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setProfileId(profileId)
                .build();

        return ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);
    }

    private static CreateManagedCardModel.Builder getDebitCorporateManagedCardModel(final String managedAccountId){
        return CreateManagedCardModel
                .DefaultCreateDebitManagedCardModel(corporateDebitManagedCardsProfileId,
                        managedAccountId)
                .setCardholderMobileNumber(String.format("%s%s",
                        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build().getRootUser().getMobile().getCountryCode(),
                        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build().getRootUser().getMobile().getNumber()));
    }

    private static CreateManagedCardModel.Builder getPrepaidCorporateManagedCardModel(){
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
                        corporateCurrency)
                .setCardholderMobileNumber(String.format("%s%s",
                        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build().getRootUser().getMobile().getCountryCode(),
                        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build().getRootUser().getMobile().getNumber()));
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
