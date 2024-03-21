package opc.junit.multi.managedcards;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardBrand;
import opc.enums.opc.FundingType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.DigitalWalletsModel;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag(MultiTags.MANAGED_CARDS_PROVISIONING)
public class CreatePushProvisioningManagedCardDigitalWalletsTests extends AbstractCreateManagedCardDigitalWalletsTests {

    @Override
    protected boolean isManualProvisioningEnabled() {
        return true;
    }

    @Override
    protected boolean isPushProvisioningEnabled() {
        return true;
    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledInCardArguments")
    public void CreateManagedCard_PropertyAndProfileConfigurationChecks_Success(final boolean isTokenisationEnabled,
                                                                                final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(false, false)
                        .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                        .build())
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, false, false, null);

    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void CreateManagedCard_PropertyAndProfileConfigurationChecksManualNotRequested_PushProvisioningNotEnabled(final boolean isTokenisationEnabled,
                                                                                                                     final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(false, true)
                        .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                        .build())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PUSHPROVISIONING_NOT_ENABLED"));

    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void CreateManagedCard_PropertyAndProfileConfigurationChecksManualRequested_PushProvisioningNotEnabled(final boolean isTokenisationEnabled,
                                                                                                                  final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(true, true)
                        .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                        .build())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PUSHPROVISIONING_NOT_ENABLED"));

    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void CreateManagedCard_PropertyAndProfileConfigurationChecksDigitalWalletsEnabledInPayload_Success(final boolean isTokenisationEnabled,
                                                                                                              final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(true, false)
                        .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                        .build())
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, true, false, MULTI_MASTERCARD_ARTWORK_REFERENCE);
    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void CreateManagedCard_PropertyAndProfileConfigurationChecksNoDigitalWalletsInPayload_Success(final boolean isTokenisationEnabled,
                                                                                                         final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(null)
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, true, false, MULTI_MASTERCARD_ARTWORK_REFERENCE);
    }

    @Test
    public void CreateManagedCard_NoConfigEnabledEnablePushProvisioning_PushProvisioningNotEnabled() {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                false, false, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                false, false, innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(false, true)
                        .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                        .build())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PUSHPROVISIONING_NOT_ENABLED"));
    }

    private static Stream<Arguments> walletsNotEnabledArguments() {
        return Stream.of(
                arguments(true, false),
                arguments(false, true),
                arguments(false, false)
        );
    }

    private static Stream<Arguments> walletsNotEnabledInCardArguments() {
        return Stream.of(
                arguments(true, true),
                arguments(true, false),
                arguments(false, true),
                arguments(false, false)
        );
    }
}