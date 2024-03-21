package opc.junit.multi.managedcards;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardBrand;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.FundingType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.multi.managedcards.DigitalWalletsModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.stream.Stream;

import static opc.enums.opc.InstrumentType.VIRTUAL;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag(MultiTags.MANAGED_CARDS_PROVISIONING)
public class UpdatePushProvisioningManagedCardDigitalWalletsTests extends AbstractUpdateManagedCardDigitalWalletsTests {

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
    public void PatchManagedCard_PropertyAndProfileConfigurationChecks_Success(final boolean isTokenisationEnabled,
                                                                               final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(false, false)
                                .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                                .build())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(consumerPrepaidManagedCardsProfileId))
                        .body("currency", equalTo(consumerCurrency))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CONSUMER.name()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonDetails(response, consumerPrepaidManagedCard, patchManagedCardModel, VIRTUAL,
                null, false, false, false, null);

    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void PatchManagedCard_PropertyAndProfileConfigurationChecksManualNotRequested_PushProvisioningNotEnabled(final boolean isTokenisationEnabled,
                                                                                                                    final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(false, true)
                                .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                                .build())
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PUSHPROVISIONING_NOT_ENABLED"));

    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void PatchManagedCard_PropertyAndProfileConfigurationChecksManualRequested_PushProvisioningNotEnabled(final boolean isTokenisationEnabled,
                                                                                                                 final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(true, true)
                                .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                                .build())
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PUSHPROVISIONING_NOT_ENABLED"));

    }

    @ParameterizedTest
    @MethodSource("walletsNotEnabledArguments")
    public void PatchManagedCard_PropertyAndProfileConfigurationChecksNoDigitalWalletsInPayload_NotEnabledSuccess(final boolean isTokenisationEnabled,
                                                                                                                  final boolean isProfileEnabled) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                true, isProfileEnabled, innovatorToken);

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, isTokenisationEnabled, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(null)
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(consumerPrepaidManagedCardsProfileId))
                        .body("currency", equalTo(consumerCurrency))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CONSUMER.name()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonDetails(response, consumerPrepaidManagedCard, patchManagedCardModel, VIRTUAL,
                null, false, false, false, null);

    }

    @Test
    public void PatchManagedCard_NoConfigEnabledEnablePushProvisioning_PushProvisioningNotEnabled() {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                false, false, adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                false, false, innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(false, true)
                                .setArtworkReference(MULTI_MASTERCARD_ARTWORK_REFERENCE)
                                .build())
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
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
