package opc.junit.smoke;

import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.CardBureau;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PinValueModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.secure.AdditionalPropertiesModel;
import opc.models.secure.DetokenizeModel;
import opc.models.secure.TokenizeModel;
import opc.models.secure.TokenizePropertiesModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
public class SecurityCardsTests extends BaseSmokeSetup {

    private static String consumerAssociateRandom;
    private static String consumerAuthenticationToken;
    private static String consumerCurrency;
    private static String corporateAssociateRandom;
    private static String corporateAuthenticationToken;
    private static String corporateCurrency;
    private static String existingCorporateCurrency;
    private static String existingConsumerCurrency;
    private static String existingConsumerAuthenticationToken;
    private static String existingCorporateAuthenticationToken;
    private static String existingConsumerAssociateRandom;
    private static String existingCorporateAssociateRandom;

    @BeforeAll
    public static void Setup() {

        consumerSetup();
        corporateSetup();

        existingConsumerAuthenticationToken = getExistingConsumerDetails().getLeft();
        existingCorporateAuthenticationToken = getExistingCorporateDetails().getLeft();
        existingCorporateCurrency = getExistingCorporateDetails().getRight();
        existingConsumerCurrency = getExistingConsumerDetails().getRight();
        existingConsumerAssociateRandom = associate(existingConsumerAuthenticationToken);
        existingCorporateAssociateRandom = associate(existingCorporateAuthenticationToken);
    }

    @Test
    public void SecurityDisabled_ConsumerCreateManagedCardDetailsTokenized_Success() {
        disableCardDetailsSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));

        enableCardDetailsSetup();
    }

    @Test
    public void SecurityDisabled_ExistingConsumerCreateManagedCardDetailsTokenized_Success() {
        disableCardDetailsSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, existingConsumerCurrency)
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, existingConsumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), existingConsumerAssociateRandom, existingConsumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), existingConsumerAssociateRandom, existingConsumerAuthenticationToken));

        enableCardDetailsSetup();
    }

    @Test
    public void SecurityDisabled_ConsumerGetManagedCardDetailsTokenized_Success() {
        disableCardDetailsSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final JsonPath jsonPath =
                ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isNotTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isNotTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));

        enableCardDetailsSetup();
    }

    @Test
    public void SecurityEnabled_ConsumerCreateManagedCardDetailsTokenized_Success() {
        enableCardDetailsSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));

        disableCardDetailsSetup();
    }

    @Test
    public void SecurityEnabled_ConsumerGetManagedCardDetailsTokenized_Success() {
        enableCardDetailsSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, consumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken);

        final JsonPath jsonPath =
                ManagedCardsService.getManagedCard(secretKey, managedCardId, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), consumerAssociateRandom, consumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), consumerAssociateRandom, consumerAuthenticationToken));

        disableCardDetailsSetup();
    }

    @Test
    public void SecurityEnabled_ExistingConsumerGetManagedCardDetailsTokenized_Success() {
        enableCardDetailsSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId, existingConsumerCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, existingConsumerAuthenticationToken);

        final JsonPath jsonPath =
                ManagedCardsService.getManagedCard(secretKey, managedCardId, existingConsumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        assertTrue(isTokenized(jsonPath.get("cardNumber.value"), existingConsumerAssociateRandom, existingConsumerAuthenticationToken));
        assertTrue(isTokenized(jsonPath.get("cvv.value"), existingConsumerAssociateRandom, existingConsumerAuthenticationToken));

        disableCardDetailsSetup();
    }

    @Test
    public void SecurityDisabled_CorporateUpgradeManagedCardPinTokenized_Success() {
        disableCardPinSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel(tokenize("9876", corporateAssociateRandom, corporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        enableCardPinSetup();
    }

    @Test
    public void SecurityDisabled_CorporateGetManagedCardPinTokenized_ReturnsPlainSuccess() {
        disableCardPinSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel(tokenize("1234", corporateAssociateRandom, corporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.activatePhysicalCard
                (new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken);

        final String pin =
                ManagedCardsService.getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("pin.value");

        assertTrue(isNotTokenized(pin, corporateAssociateRandom, corporateAuthenticationToken));

        enableCardPinSetup();
    }

    @Test
    public void SecurityDisabled_ExistingCorporateGetManagedCardPinTokenized_ReturnsPlainSuccess() {
        disableCardPinSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, existingCorporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, existingCorporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel(tokenize("1234", existingCorporateAssociateRandom, existingCorporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, existingCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.activatePhysicalCard
                (new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, existingCorporateAuthenticationToken);

        final String pin =
                ManagedCardsService.getPhysicalCardPin(secretKey, managedCardId, existingCorporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("pin.value");

        assertTrue(isNotTokenized(pin, existingCorporateAssociateRandom, existingCorporateAuthenticationToken));

        enableCardPinSetup();
    }

    @Test
    public void SecurityEnabled_CorporateUpgradeManagedCardPinTokenized_Success() {
        enableCardPinSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(tokenize("1234", corporateAssociateRandom, corporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        disableCardPinSetup();
    }

    @Test
    public void SecurityEnabled_ExistingCorporateUpgradeManagedCardPinTokenized_Success() {
        enableCardPinSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, existingCorporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, existingCorporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(tokenize("1234", existingCorporateAssociateRandom, existingCorporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, existingCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        disableCardPinSetup();
    }

    @Test
    public void SecurityEnabled_CorporateGetManagedCardPinTokenized_ReturnsTokenizedSuccess() {
        enableCardPinSetup();

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(tokenize("1234", corporateAssociateRandom, corporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.activatePhysicalCard
                (new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken);

        final String pin =
                ManagedCardsService.getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("pin.value");

        assertTrue(isTokenized(pin, corporateAssociateRandom, corporateAuthenticationToken));

        disableCardPinSetup();
    }


    private static void updateSecurityModel(final Map<String, Boolean> securityModelConfig) {
        final UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setSecurityModelConfig(securityModelConfig)
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateProgramme(updateProgrammeModel, programmeId, AdminService.loginAdmin()),
                SC_OK);
    }

    private static String tokenize(final String value, final String associateRandom, final String authenticationToken) {

        return tokenize(value, associateRandom, authenticationToken, sharedKey);
    }

    private static String tokenize(final String value, final String associateRandom, final String authenticationToken, final String sharedKey) {

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(associateRandom)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue(value)
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        return SecureService.tokenize(sharedKey, authenticationToken, tokenizeModel)
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");
    }

    private boolean isNotTokenized(final String token, final String associateRandom, final String authenticationToken) {

        return isNotTokenized(token, associateRandom, authenticationToken, sharedKey);
    }

    private boolean isNotTokenized(final String token, final String associateRandom, final String authenticationToken, final String sharedKey) {

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(token)
                        .setRandom(associateRandom)
                        .build();

        return SecureService.detokenize(sharedKey,
                authenticationToken, detokenizeModel).statusCode() == SC_BAD_REQUEST;
    }

    private boolean isTokenized(final String token, final String associateRandom, final String authenticationToken) {

        return isTokenized(token, associateRandom, authenticationToken, sharedKey);
    }

    private boolean isTokenized(final String token, final String associateRandom, final String authenticationToken, final String sharedKey) {

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(token)
                        .setRandom(associateRandom)
                        .build();

        return SecureService.detokenize(sharedKey,
                authenticationToken, detokenizeModel).statusCode() == SC_OK;
    }

    private static String associate(final String authenticationToken) {

        return associate(authenticationToken, sharedKey);
    }

    private static String associate(final String authenticationToken, final String sharedKey) {

        return SecureService.associate(sharedKey, authenticationToken, Optional.empty())
                .then().statusCode(SC_OK).extract().jsonPath().get("random");
    }

    private static void enableCardDetailsSetup() {
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);

    }

    private static void disableCardDetailsSetup() {
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), false,
                        SecurityModelConfiguration.CARD_NUMBER.name(), false);

        updateSecurityModel(securityModelConfig);
    }

    private static void disableCardPinSetup() {
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), false,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);
    }

    private static void enableCardPinSetup() {
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        consumerAssociateRandom = associate(consumerAuthenticationToken);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        corporateAssociateRandom = associate(corporateAuthenticationToken);
    }

}
