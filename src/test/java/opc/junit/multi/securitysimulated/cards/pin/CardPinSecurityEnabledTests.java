package opc.junit.multi.securitysimulated.cards.pin;

import com.google.common.collect.ImmutableMap;
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

import static org.apache.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CardPinSecurityEnabledTests extends BaseSecurityConfigurationTests {

    @BeforeAll
    public static void Setup(){
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
    }

    @Test
    public void SecurityEnabled_UpgradeManagedCardPinTokenized_Success(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
                        .setPin(new PinValueModel(tokenize("1234", SecurityModelConfiguration.PIN, corporateAuthenticationToken)))
                        .build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SecurityEnabled_UpgradeManagedCardPinNonTokenized_BadRequest(){

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
    public void SecurityEnabled_GetManagedCardPinTokenized_ReturnsTokenizedSuccess(){

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId, corporateCurrency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken);

        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(TestHelper.VERIFICATION_CODE)
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

        assertTrue(isTokenized(pin, SecurityModelConfiguration.PIN, corporateAuthenticationToken));
    }
}
