package opc.junit.multi.managedcards;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardBureau;
import opc.enums.opc.DeliveryMethod;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedCardMode;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.PinValueModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.multi.users.UsersModel;
import opc.models.simulator.AdditionalPropertiesModel;
import opc.models.simulator.DetokenizeModel;
import opc.models.simulator.TokenizeModel;
import opc.models.simulator.TokenizePropertiesModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class UpgradeManagedCardToPhysicalTests extends BaseManagedCardsSetup {

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
    public void UpgradeManagedCard_PrepaidCorporate_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_PrepaidConsumer_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken),
                managedCard,
                consumerPrepaidManagedCardsProfileId,
                IdentityType.CONSUMER, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_DebitCorporate_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                        .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporateDebitManagedCardsProfileId, IdentityType.CORPORATE, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_DebitConsumer_Success(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(consumerManagedAccountsProfileId, consumerDebitManagedCardsProfileId,
                        consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                        .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken),
                managedCard,
                consumerDebitManagedCardsProfileId,
                IdentityType.CONSUMER, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_CorporateUser_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getRight()),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_AllDetails_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final String pin = "9876";

        final String tokenizedPin =
                SimulatorService.tokenize(secretKey,
                new TokenizeModel(new TokenizePropertiesModel(new AdditionalPropertiesModel(pin, "PIN"))), corporateAuthenticationToken)
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE)
                        .setProductReference(CardBureau.NITECREST.getProductReference())
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel(tokenizedPin))
                        .build();

        assertSuccessfulResponse(ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, 0, upgradeToPhysicalCardModel);

        ManagedCardsHelper.activateManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final String retrievedCardPin =
                ManagedCardsService
                        .getPhysicalCardPin(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath().get("pin.value");


        final String detokenizedPin =
                SimulatorService.detokenize(secretKey,
                        new DetokenizeModel(retrievedCardPin, "PIN"), corporateAuthenticationToken)
                        .then().statusCode(SC_OK).extract().jsonPath().get("value");

        Assertions.assertEquals(pin, detokenizedPin);
    }

    @Test
    public void UpgradeManagedCard_CardWithFunds_Success(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final Long cardBalance = 20L;

        transferFundsToCard(corporateAuthenticationToken, IdentityType.CORPORATE, managedCard.getManagedCardId(),
                corporateCurrency, cardBalance, 1);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                        .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE, cardBalance.intValue(), upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2NotRequired_Success(){
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE)
                .setNameOnCardLine2(null)
                .build();

        assertSuccessfulResponse(ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken),
            managedCard,
            consumerPrepaidManagedCardsProfileId,
            IdentityType.CONSUMER, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_DebitParentManagedAccountBlocked_ParentManagedAccountBlocked(){
        final ManagedCardDetails managedCard =
                createManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedCard.getManagedCardModel().getParentManagedAccountId(),
                secretKey, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_BLOCKED"));
    }

    @Test
    public void UpgradeManagedCard_UnknownProductReference_ProductReferenceInvalid(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setProductReference(RandomStringUtils.randomAlphabetic(6))
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PRODUCT_REFERENCE_INVALID"));
    }

    @Test
    public void UpgradeManagedCard_UnknownCarrierType_CarrierTypeInvalid(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(RandomStringUtils.randomAlphabetic(6))
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CARRIER_TYPE_INVALID"));
    }

    @Test
    public void UpgradeManagedCard_InvalidActivationCode_BadRequest(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setActivationCode(RandomStringUtils.randomNumeric(10))
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpgradeManagedCard_UnknownTokenizedPin_NotFound(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(RandomStringUtils.randomAlphabetic(24)))
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UpgradeManagedCard_ShortTokenizedPin_BadRequest(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(RandomStringUtils.randomAlphabetic(23)))
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("SIZE"))
                .body("invalidFields[0].fieldName", equalTo("token.token"))
                .body("invalidFields[0].params[0]", equalTo("24"))
                .body("invalidFields[0].params[1]", equalTo("24"));
    }

    @Test
    public void UpgradeManagedCard_LongTokenizedPin_BadRequest(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(RandomStringUtils.randomAlphabetic(25)))
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.pin.value: size must be between 4 and 24"));
    }

    @Test
    public void UpgradeManagedCard_NoAddress_BadRequest(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setDeliveryAddress(null)
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpgradeManagedCard_NoActivationCode_BadRequest(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setActivationCode(null)
                        .build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpgradeManagedCard_AlreadyUpgradedAndActivated_InstrumentAlreadyUpgradedToPhysical(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedCardsHelper
                .upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void UpgradeManagedCard_AlreadyUpgraded_InstrumentAlreadyUpgradedToPhysical(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedCardsHelper
                .upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void UpgradeManagedCard_CardReplacementForANonActiveCardReportedLostOrStolen_InstrumentAlreadyUpgradedToPhysical(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

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

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, replacementCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void UpgradeManagedCard_CardBlocked_InstrumentBlocked(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_BLOCKED"));
    }

    @Test
    public void UpgradeManagedCard_CardDestroyed_InstrumentDestroyed(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DESTROYED"));
    }

    @Test
    public void UpgradeManagedCard_CorporateUpgradeUserCard_Success(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, user.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        assertSuccessfulResponse(ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken),
                managedCard,
                corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, 0, upgradeToPhysicalCardModel);
    }

    @Test
    public void UpgradeManagedCard_UserUpgradeCorporateCard_Unauthorized(){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), user.getLeft())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpgradeManagedCard_UnknownManagedCardId_NotFound() {
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @DisplayName("UpgradeManagedCard_NoManagedCardId_NotFound - DEV-2808 opened to return 404")
    public void UpgradeManagedCard_NoManagedCardId_NotFound() {
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void UpgradeManagedCard_CrossIdentityCheck_NotFound(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UpgradeManagedCard_InvalidApiKey_Unauthorised(){
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, "abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpgradeManagedCard_NoApiKey_BadRequest(){
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, "", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpgradeManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpgradeManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpgradeManagedCard_BackofficeCorporateImpersonator_Forbidden(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpgradeManagedCard_BackofficeConsumerImpersonator_Forbidden(){
        final ManagedCardDetails managedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(),
                        getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"COURIER", "REGISTERED_MAIL"})
    public void UpgradeManagedCard_BulkDelivery_Success(final String deliveryMethod){
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final String contactNumber = TestHelper.generateRandomValidMobileNumber(10);
        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().setContactNumber(contactNumber).build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE, deliveryMethod).setBulkDelivery(true).build();

        final Response response = ManagedCardsService
            .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);

        response.then()
            .body("physicalCardDetails.bulkDelivery", equalTo(true))
            .body("physicalCardDetails.deliveryAddress.contactNumber", equalTo(contactNumber));

        assertSuccessfulResponse(response, managedCard,
            corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE, 0, upgradeToPhysicalCardModel, deliveryMethod);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"358261252", "abcdef"})
    public void UpgradeManagedCard_BulkDeliveryInvalidContactNumber_BadRequest(final String contactNumber){
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().setContactNumber(contactNumber).build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE, "COURIER").setBulkDelivery(true).build();

        ManagedCardsService
            .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.deliveryAddress.contactNumber: must match \"^\\+[0-9]+$\""));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"+358", "+35812345623456234621"})
    public void UpgradeManagedCard_BulkDeliveryInvalidContactNumberSize_BadRequest(final String contactNumber){
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().setContactNumber(contactNumber).build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE, "COURIER").setBulkDelivery(true).build();

        ManagedCardsService
            .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.deliveryAddress.contactNumber: size must be between 5 and 20"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"STANDARD_DELIVERY", "FIRST_CLASS_MAIL"})
    public void UpgradeManagedCard_BulkDeliveryDeliveryMethodInvalid_Conflict(final String deliveryMethod){
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().setContactNumber(TestHelper.generateRandomValidMobileNumber(10)).build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE, deliveryMethod).setBulkDelivery(true).build();

        ManagedCardsService
            .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("DELIVERY_METHOD_INVALID"));
    }

    @Test
    public void UpgradeManagedCard_BulkDeliveryContactNumberNotProvided_Conflict(){
        final ManagedCardDetails managedCard =
            createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE, "COURIER").setBulkDelivery(true).build();

        ManagedCardsService
            .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CONTACT_NUMBER_NEEDED_FOR_BULK_DELIVERY"));
    }

    @Test
    public void UpgradeManagedCard_PrepaidRenewCard_Success(){
        final CreateManagedCardModel createManagedCardModel =
            CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
                    corporateCurrency)
                .setRenewalType("RENEW")
                .build();

        final String managedCardId =
            ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
            UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, TestHelper.VERIFICATION_CODE).build();

        final ValidatableResponse response = ManagedCardsService
                .upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        final String cvv = response.extract().jsonPath().get("cvv.value");
        final Long renewalTimestamp = response.extract().jsonPath().get("renewalTimestamp");

        SimulatorService.simulateRenew(managedCardId, secretKey)
            .then()
            .statusCode(SC_NO_CONTENT);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE);

        ManagedCardsService
            .activatePhysicalCard(activatePhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
            .then()
            .statusCode(SC_OK);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
            .then()
            .statusCode(SC_OK)
            .body("cvv.value", is(not(equalTo(cvv))))
            .body("renewalTimestamp", is(greaterThan((renewalTimestamp))));
    }

    private void assertSuccessfulResponse(final Response response,
                                          final ManagedCardDetails managedCardDetails,
                                          final String managedCardProfileId,
                                          final IdentityType identityType,
                                          final int balance,
                                          final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel){
        response
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(managedCardProfileId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(managedCardDetails.getManagedCardModel().getTag()))
                .body("friendlyName", equalTo(managedCardDetails.getManagedCardModel().getFriendlyName()))
                .body("state.state", equalTo("ACTIVE"))
                .body("type", equalTo("VIRTUAL"))
                .body("cardBrand", equalTo("MASTERCARD"))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", equalTo("522093"))
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
                .body("physicalCardDetails.pendingActivation", equalTo(true))
                .body("physicalCardDetails.pinBlocked", equalTo(false))
                .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                .body("physicalCardDetails.deliveryAddress.name", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getName()))
                .body("physicalCardDetails.deliveryAddress.surname", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getSurname()))
                .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getAddressLine1()))
                .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getAddressLine2()))
                .body("physicalCardDetails.deliveryAddress.city", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getCity()))
                .body("physicalCardDetails.deliveryAddress.postCode", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getPostCode()))
                .body("physicalCardDetails.deliveryAddress.state", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getState()))
                .body("physicalCardDetails.deliveryAddress.country", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getCountry()))
                .body("physicalCardDetails.deliveryMethod", equalTo(DeliveryMethod.STANDARD_DELIVERY.name()))
                .body("physicalCardDetails.nameOnCardLine2", equalTo(upgradeToPhysicalCardModel.getNameOnCardLine2()))
                .body("nameOnCardLine2", equalTo(upgradeToPhysicalCardModel.getNameOnCardLine2()))
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

    private void assertSuccessfulResponse(final Response response,
                                          final ManagedCardDetails managedCardDetails,
                                          final String managedCardProfileId,
                                          final IdentityType identityType,
                                          final int balance,
                                          final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel,
                                          final String deliveryMethod){
        response
            .then()
            .statusCode(SC_OK)
            .body("id", notNullValue())
            .body("profileId", equalTo(managedCardProfileId))
            .body("externalHandle", notNullValue())
            .body("tag", equalTo(managedCardDetails.getManagedCardModel().getTag()))
            .body("friendlyName", equalTo(managedCardDetails.getManagedCardModel().getFriendlyName()))
            .body("state.state", equalTo("ACTIVE"))
            .body("type", equalTo("VIRTUAL"))
            .body("cardBrand", equalTo("MASTERCARD"))
            .body("cardNumber.value", notNullValue())
            .body("cvv.value", notNullValue())
            .body("cardNumberFirstSix", equalTo("522093"))
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
            .body("physicalCardDetails.pendingActivation", equalTo(true))
            .body("physicalCardDetails.pinBlocked", equalTo(false))
            .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
            .body("physicalCardDetails.deliveryAddress.name", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getName()))
            .body("physicalCardDetails.deliveryAddress.surname", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getSurname()))
            .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getAddressLine1()))
            .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getAddressLine2()))
            .body("physicalCardDetails.deliveryAddress.city", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getCity()))
            .body("physicalCardDetails.deliveryAddress.postCode", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getPostCode()))
            .body("physicalCardDetails.deliveryAddress.state", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getState()))
            .body("physicalCardDetails.deliveryAddress.country", equalTo(upgradeToPhysicalCardModel.getDeliveryAddress().getCountry()))
            .body("physicalCardDetails.deliveryMethod", equalTo(deliveryMethod))
            .body("physicalCardDetails.nameOnCardLine2", equalTo(upgradeToPhysicalCardModel.getNameOnCardLine2()))
            .body("nameOnCardLine2", equalTo(upgradeToPhysicalCardModel.getNameOnCardLine2()))
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

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
