package opc.junit.multi.securitysimulated.cards.pin;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.CardBureau;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.securitysimulated.BaseSecurityConfigurationTests;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PinValueModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CardPinSecurityDisabledTests extends BaseSecurityConfigurationTests {

    @BeforeAll
    public static void Setup(){
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), false,
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
    }

    @Test
    public void SecurityDisabled_UpgradeManagedCardPinTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel(tokenize("9876", SecurityModelConfiguration.PIN, corporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SecurityDisabled_UpgradeManagedCardPinNonTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel("1234"))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SecurityDisabled_GetManagedCardPinTokenized_ReturnsPlainSuccess(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel(tokenize("1234", SecurityModelConfiguration.PIN, corporateAuthenticationToken)))
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

        assertTrue(isNotTokenized(pin, SecurityModelConfiguration.PIN, corporateAuthenticationToken));
    }

    @Test
    public void SecurityDisabled_GetManagedCardPinNonTokenized_ReturnsPlainSuccess(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setCarrierType(CardBureau.NITECREST.getCarrierType())
                        .setPin(new PinValueModel("1234"))
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

        assertTrue(isNotTokenized(pin, SecurityModelConfiguration.PIN, corporateAuthenticationToken));
    }
}
