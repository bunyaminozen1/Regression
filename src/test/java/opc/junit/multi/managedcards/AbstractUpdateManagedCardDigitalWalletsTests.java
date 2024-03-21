package opc.junit.multi.managedcards;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.DigitalWalletsModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractUpdateManagedCardDigitalWalletsTests extends BaseManagedCardsSetup {

    protected abstract boolean isManualProvisioningEnabled();
    protected abstract boolean isPushProvisioningEnabled();

    protected static String corporateAuthenticationToken;
    protected static String consumerAuthenticationToken;
    protected static String corporateCurrency;
    protected static String consumerCurrency;

    protected static ManagedCardDetails consumerPrepaidManagedCard;
    protected static ManagedCardDetails corporateDebitManagedCard;
    protected static String adminToken;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();

        adminToken = AdminService.loginAdmin();
    }

    @BeforeEach
    public void resetConfiguration(){
        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                null, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, null,
                null, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                null, adminToken);

        consumerPrepaidManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateDebitManagedCard =
                createManagedAccountAndPhysicalDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId,
                        corporateCurrency, corporateAuthenticationToken);
    }

    @ParameterizedTest
    @MethodSource("artworkCardEnabledSuccessfulArguments")
    public void PatchManagedCard_PrepaidConsumerCardProvisioningEnabled_Success(final List<String> defaultArtwork,
                                                                           final List<String> innovatorArtwork,
                                                                           final String artworkReference,
                                                                           final CardBrand cardBrand,
                                                                           final String expectedArtworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                                .setArtworkReference(artworkReference)
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
                null, false, isManualProvisioningEnabled(), isPushProvisioningEnabled(), expectedArtworkReference);
    }

    @ParameterizedTest
    @MethodSource("artworkCardEnabledSuccessfulArguments")
    public void PatchManagedCard_DebitCorporateCardProvisioningEnabled_Success(final List<String> defaultArtwork,
                                                                          final List<String> innovatorArtwork,
                                                                          final String artworkReference,
                                                                          final CardBrand cardBrand,
                                                                          final String expectedArtworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporateDebitManagedCardsProfileId, FundingType.DEBIT,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                                .setArtworkReference(artworkReference)
                                .build())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, corporateDebitManagedCard.getManagedCardId(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("profileId", equalTo(corporateDebitManagedCardsProfileId))
                        .body("cardLevelClassification", equalTo(CardLevelClassification.CORPORATE.name()))
                        .body("parentManagedAccountId", equalTo(corporateDebitManagedCard.getManagedCardModel()
                                .getParentManagedAccountId()));

        assertCommonDetails(response, corporateDebitManagedCard, patchManagedCardModel, PHYSICAL,
                corporateDebitManagedCard.getPhysicalCardAddressModel(), true, isManualProvisioningEnabled(), isPushProvisioningEnabled(), expectedArtworkReference);
    }

    @ParameterizedTest
    @MethodSource("artworkCardNotEnabledSuccessfulArguments")
    public void PatchManagedCard_CardProvisioningNotEnabled_Success(final List<String> defaultArtwork,
                                                               final List<String> innovatorArtwork,
                                                               final String artworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(false, false)
                                .setArtworkReference(artworkReference)
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
    @MethodSource("artworkInvalidArguments")
    public void PatchManagedCard_CardProvisioningEnabled_WalletArtworkInvalid(final List<String> defaultArtwork,
                                                                        final List<String> innovatorArtwork,
                                                                        final String artworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                                .setArtworkReference(artworkReference)
                                .build())
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLET_ARTWORK_INVALID"));
    }

    @ParameterizedTest
    @MethodSource("artworkNotSpecifiedArguments")
    public void PatchManagedCard_CardProvisioningEnable_WalletArtworkNotSpecified(final List<String> defaultArtwork,
                                                                             final List<String> innovatorArtwork,
                                                                             final String artworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                                .setArtworkReference(artworkReference)
                                .build())
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, consumerPrepaidManagedCard.getManagedCardId(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLET_ARTWORK_NOT_SPECIFIED"));
    }

    @ParameterizedTest
    @MethodSource("artworkNoDigitalWalletsInPayloadSuccessfulArguments")
    public void PatchManagedCard_PrepaidConsumerNoDigitalWalletsInPayload_NotEnabledSuccess(final List<String> defaultArtwork,
                                                                                            final List<String> innovatorArtwork,
                                                                                            final CardBrand cardBrand) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

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
    public void PatchManagedCard_ManualProvisioningEnabled_Success() {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(true, false).build())
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
                null, false, true, false, MULTI_MASTERCARD_ARTWORK_REFERENCE);
    }

    @ParameterizedTest
    @MethodSource("artworkCardNotEnabledSuccessfulArguments")
    public void PatchManagedCard_NoProvisioningEnabled_Success(final List<String> defaultArtwork,
                                                                final List<String> innovatorArtwork,
                                                                final String artworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setDigitalWallets(DigitalWalletsModel.builder(false, false)
                                .setArtworkReference(artworkReference)
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
    @MethodSource("artworkNoDigitalWalletsInPayloadNotEnabledArguments")
    public void PatchManagedCard_PrepaidConsumerNoDigitalWalletsInPayload_NotEnabledSuccess(final List<String> defaultArtwork,
                                                                                            final List<String> innovatorArtwork) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerPrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

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

    private static Stream<Arguments> artworkCardEnabledSuccessfulArguments() {
        return Stream.of(
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                        Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), MULTI_MASTERCARD_ARTWORK_REFERENCE, CardBrand.MASTERCARD, MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE),
                        Arrays.asList(MULTI_MASTERCARD_ARTWORK_REFERENCE, MULTI_MASTERCARD_ARTWORK_REFERENCE2), MULTI_MASTERCARD_ARTWORK_REFERENCE, CardBrand.MASTERCARD, MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                        Arrays.asList(MULTI_MASTERCARD_ARTWORK_REFERENCE, MULTI_MASTERCARD_ARTWORK_REFERENCE2), MULTI_MASTERCARD_ARTWORK_REFERENCE2, CardBrand.MASTERCARD, MULTI_MASTERCARD_ARTWORK_REFERENCE2),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), null, null, null, DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                arguments(null, null, null, null, DEFAULT_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), MULTI_MASTERCARD_ARTWORK_REFERENCE, CardBrand.MASTERCARD, MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_ARTWORK_REFERENCE), MULTI_ARTWORK_REFERENCE, null, MULTI_ARTWORK_REFERENCE),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_ARTWORK_REFERENCE), null, null, MULTI_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_ARTWORK_REFERENCE), null, null, MULTI_ARTWORK_REFERENCE)
        );
    }

    private static Stream<Arguments> artworkCardNotEnabledSuccessfulArguments() {
        return Stream.of(
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), "Test")
        );
    }

    private static Stream<Arguments> artworkNotSpecifiedArguments() {
        return Stream.of(
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                        Arrays.asList(MULTI_MASTERCARD_ARTWORK_REFERENCE, MULTI_MASTERCARD_ARTWORK_REFERENCE2), null)
        );
    }

    private static Stream<Arguments> artworkInvalidArguments() {
        return Stream.of(
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), "Test"),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), "Test1"),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                        Arrays.asList(MULTI_MASTERCARD_ARTWORK_REFERENCE, MULTI_MASTERCARD_ARTWORK_REFERENCE2), "Test1"),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), null, MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), "Test"),
                arguments(null, Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                arguments(null, null, MULTI_MASTERCARD_ARTWORK_REFERENCE)
        );
    }

    private static Stream<Arguments> artworkNoDigitalWalletsInPayloadSuccessfulArguments() {
        return Stream.of(
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                        Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), CardBrand.MASTERCARD),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), null, null),
                arguments(null, null, null),
                arguments(null, Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), CardBrand.MASTERCARD),
                arguments(null, Collections.singletonList(MULTI_ARTWORK_REFERENCE), null),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_ARTWORK_REFERENCE), null),
                arguments(null, Collections.singletonList(MULTI_ARTWORK_REFERENCE), null)
        );
    }

    private static Stream<Arguments> artworkNoDigitalWalletsInPayloadNotEnabledArguments() {
        return Stream.of(
                arguments(Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE),
                        Arrays.asList(MULTI_MASTERCARD_ARTWORK_REFERENCE, MULTI_MASTERCARD_ARTWORK_REFERENCE2))
        );
    }

    protected void assertCommonDetails(final ValidatableResponse response,
                                     final ManagedCardDetails managedCard,
                                     final PatchManagedCardModel patchManagedCardModel,
                                     final InstrumentType instrumentType,
                                     final PhysicalCardAddressModel physicalCardAddressModel,
                                     final boolean isPhysical,
                                     final boolean isWalletsEnabled,
                                     final boolean isPushProvisioningEnabled,
                                     final String artworkReference) {

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
                .body("digitalWallets.walletsEnabled", equalTo(isWalletsEnabled))
                .body("digitalWallets.pushProvisioningEnabled", equalTo(isPushProvisioningEnabled))
                .body("digitalWallets.artworkReference", equalTo(artworkReference))
                .body("mode", equalTo(managedCard.getManagedCardModel().getMode()));

        assertBillingAddress(response, managedCard, patchManagedCardModel);

        if(isPhysical){
            assertPhysicalCardDetails(response, physicalCardAddressModel);
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
                                           final PhysicalCardAddressModel physicalCardAddressModel){
        response
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
                .body("physicalCardDetails.deliveryMethod", equalTo(DeliveryMethod.STANDARD_DELIVERY.name()))
                .body("physicalCardDetails.manufacturingState", equalTo(ManufacturingState.DELIVERED.name()));
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.USD.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }
}
