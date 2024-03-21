package opc.junit.multi.security.cards.pin;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.security.BaseSecurityConfigurationTests;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PinValueModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.secure.DetokenizeModel;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CardPinSecurityEnabledTests extends BaseSecurityConfigurationTests {

    @BeforeAll
    public static void Setup() {
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        corporateAssociateRandom = associate(corporateAuthenticationToken);
    }

    @Test
    public void SecurityEnabled_UpgradeManagedCardPinTokenized_Success() {

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
    }

    @Test
    public void SecurityEnabled_UpgradeManagedCardPinNonTokenized_BadRequest() {

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel("1234"))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SecurityEnabled_GetManagedCardPinTokenized_ReturnsTokenizedSuccess() {

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

        ManagedCardsHelper.activateManagedCard(secretKey, managedCardId, corporateAuthenticationToken);

        final String pin =
                ManagedCardsService.getPhysicalCardPin(secretKey, managedCardId, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("pin.value");

        assertTrue(isTokenized(pin, corporateAssociateRandom, corporateAuthenticationToken));
    }

    @Test
    public void SecurityEnabled_DetokenizePinScaEnabledCard_Success() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(scaMcApp.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, scaMcApp.getSecretKey());

        final String corporateAssociateRandom = associate(corporate.getRight(), scaMcApp.getSharedKey());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(scaMcApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), createCorporateModel.getBaseCurrency())
                        .build();

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                scaMcApp.getSecretKey(), corporate.getRight());

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, scaMcApp.getSecretKey(), corporate.getRight());

        final String pinValue = "9834";
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(tokenize(pinValue, corporateAssociateRandom, corporate.getRight(), scaMcApp.getSharedKey())))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, scaMcApp.getSecretKey(), managedCardId, corporate.getRight())
                .then()
                .statusCode(SC_OK);

        ManagedCardsHelper.activateManagedCard(scaMcApp.getSecretKey(), managedCardId, corporate.getRight());

        final String pin =
                ManagedCardsHelper.getPhysicalCardPin(scaMcApp.getSecretKey(), managedCardId, corporate.getRight());

        AuthenticationHelper.logout(corporate.getRight(), scaMcApp.getSecretKey());
        TimeUnit.SECONDS.sleep(31);
        final String newToken = AuthenticationHelper.login(createCorporateModel.getRootUser().getEmail(), scaMcApp.getSecretKey());
        final String newAssociateRandom = associate(newToken, scaMcApp.getSharedKey());

        final DetokenizeModel.Builder detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(pin)
                        .setRandom(newAssociateRandom);

        SecureService.detokenize(scaMcApp.getSharedKey(), newToken, detokenizeModel.build())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup("123456", EnrolmentChannel.SMS.name(), scaMcApp.getSecretKey(), newToken);

        final String detokenizedPin =
                SecureService.detokenize(scaMcApp.getSharedKey(), newToken, detokenizeModel.build())
                        .then()
                        .statusCode(SC_OK).extract().jsonPath().getString("value");

        assertEquals(pinValue, detokenizedPin);

        AuthenticationHelper.logout(newToken, scaMcApp.getSecretKey());
        TimeUnit.SECONDS.sleep(31);
        final String newerToken = AuthenticationHelper.login(createCorporateModel.getRootUser().getEmail(), scaMcApp.getSecretKey());
        final String newerAssociateRandom = associate(newerToken, scaMcApp.getSharedKey());

        SecureService.detokenize(scaMcApp.getSharedKey(), newerToken, detokenizeModel.setRandom(newerAssociateRandom).build())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }
}
