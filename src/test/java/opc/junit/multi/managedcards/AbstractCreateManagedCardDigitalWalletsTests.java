package opc.junit.multi.managedcards;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardBrand;
import commons.enums.Currency;
import opc.enums.opc.FundingType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.DigitalWalletsModel;
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
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractCreateManagedCardDigitalWalletsTests extends BaseManagedCardsSetup{

    protected abstract boolean isManualProvisioningEnabled();
    protected abstract boolean isPushProvisioningEnabled();

    protected static String corporateAuthenticationToken;
    protected static String consumerAuthenticationToken;
    protected static String corporateCurrency;
    protected static String consumerCurrency;
    protected static CreateCorporateModel corporateDetails;
    protected static CreateConsumerModel consumerDetails;
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
    }

    @ParameterizedTest
    @MethodSource("artworkCardEnabledSuccessfulArguments")
    public void CreateManagedCard_PrepaidCorporateProvisioningEnabled_Success(final List<String> defaultArtwork,
                                                                              final List<String> innovatorArtwork,
                                                                              final String artworkReference,
                                                                              final CardBrand cardBrand,
                                                                              final String expectedArtworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                        .setArtworkReference(artworkReference)
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
                IdentityType.CORPORATE, isManualProvisioningEnabled(), isPushProvisioningEnabled(), expectedArtworkReference);
    }

    @ParameterizedTest
    @MethodSource("artworkNoDigitalWalletsInPayloadSuccessfulArguments")
    public void CreateManagedCard_PrepaidCorporateNoDigitalWalletsInPayload_Success(final List<String> defaultArtwork,
                                                                                    final List<String> innovatorArtwork,
                                                                                    final CardBrand cardBrand,
                                                                                    final String expectedArtworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

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
                IdentityType.CORPORATE, isManualProvisioningEnabled(), isPushProvisioningEnabled(), expectedArtworkReference);
    }

    @ParameterizedTest
    @MethodSource("artworkNoDigitalWalletsInPayloadSuccessfulArguments")
    public void CreateManagedCard_DebitConsumerNoDigitalWalletsInPayload_Success(final List<String> defaultArtwork,
                                                                                 final List<String> innovatorArtwork,
                                                                                 final CardBrand cardBrand,
                                                                                 final String expectedArtworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerDebitManagedCardsProfileId, FundingType.DEBIT,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

        final String managedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId)
                .setDigitalWallets(null)
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, consumerDebitManagedCardsProfileId,
                IdentityType.CONSUMER, isManualProvisioningEnabled(), isPushProvisioningEnabled(), expectedArtworkReference);
    }

    @ParameterizedTest
    @MethodSource("artworkCardEnabledSuccessfulArguments")
    public void CreateManagedCard_DebitConsumerProvisioningEnabled_Success(final List<String> defaultArtwork,
                                                                          final List<String> innovatorArtwork,
                                                                          final String artworkReference,
                                                                          final CardBrand cardBrand,
                                                                          final String expectedArtworkReference) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, consumerDebitManagedCardsProfileId, FundingType.DEBIT,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, cardBrand,
                innovatorArtwork, adminToken);

        final String managedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId)
                .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                        .setArtworkReference(artworkReference)
                        .build())
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, consumerDebitManagedCardsProfileId,
                IdentityType.CONSUMER, isManualProvisioningEnabled(), isPushProvisioningEnabled(), expectedArtworkReference);
    }

    @ParameterizedTest
    @MethodSource("artworkCardNotEnabledSuccessfulArguments")
    public void CreateManagedCard_NoProvisioningEnabled_Success(final List<String> defaultArtwork,
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

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(false, false)
                        .setArtworkReference(artworkReference)
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

    @Test
    public void CreateManagedCard_ManualProvisioningEnabled_Success() {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

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
    @MethodSource("artworkInvalidArguments")
    public void CreateManagedCard_ProvisioningEnabled_WalletArtworkInvalid(final List<String> defaultArtwork,
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

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                        .setArtworkReference(artworkReference)
                        .build())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLET_ARTWORK_INVALID"));
    }

    @ParameterizedTest
    @MethodSource("artworkNotSpecifiedArguments")
    public void CreateManagedCard_ProvisioningEnabled_WalletArtworkNotSpecified(final List<String> defaultArtwork,
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

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(DigitalWalletsModel.builder(isManualProvisioningEnabled(), isPushProvisioningEnabled())
                        .setArtworkReference(artworkReference)
                        .build())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLET_ARTWORK_NOT_SPECIFIED"));
    }

    @ParameterizedTest
    @MethodSource("artworkNoDigitalWalletsInPayloadNotEnabledArguments")
    public void CreateManagedCard_PrepaidCorporateNoDigitalWalletsInPayload_WalletArtworkNotSpecified(final List<String> defaultArtwork,
                                                                                                      final List<String> innovatorArtwork) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setDigitalWallets(null)
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLET_ARTWORK_NOT_SPECIFIED"));
    }

    @ParameterizedTest
    @MethodSource("artworkNoDigitalWalletsInPayloadNotEnabledArguments")
    public void CreateManagedCard_DebitConsumerNoDigitalWalletsInPayload_WalletArtworkNotSpecified(final List<String> defaultArtwork,
                                                                                                   final List<String> innovatorArtwork) {

        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), adminToken);

        InnovatorHelper.updateProfileWallets(programmeId, corporatePrepaidManagedCardsProfileId, FundingType.PREPAID,
                isManualProvisioningEnabled(), isPushProvisioningEnabled(), innovatorToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(null, CardBrand.MASTERCARD,
                defaultArtwork, adminToken);

        AdminHelper.setManagedCardDigitalWalletArtwork(innovatorId, CardBrand.MASTERCARD,
                innovatorArtwork, adminToken);

        final String managedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId)
                .setDigitalWallets(null)
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("WALLET_ARTWORK_NOT_SPECIFIED"));
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

    private static Stream<Arguments> artworkNoDigitalWalletsInPayloadSuccessfulArguments() {
        return Stream.of(
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                        Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), CardBrand.MASTERCARD, MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), null, null, DEFAULT_MASTERCARD_ARTWORK_REFERENCE),
                arguments(null, null, null, DEFAULT_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE), CardBrand.MASTERCARD, MULTI_MASTERCARD_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_ARTWORK_REFERENCE), null, MULTI_ARTWORK_REFERENCE),
                arguments(Collections.singletonList(DEFAULT_MASTERCARD_ARTWORK_REFERENCE), Collections.singletonList(MULTI_ARTWORK_REFERENCE), null, MULTI_ARTWORK_REFERENCE),
                arguments(null, Collections.singletonList(MULTI_ARTWORK_REFERENCE), null, MULTI_ARTWORK_REFERENCE)
        );
    }

    private static Stream<Arguments> artworkNoDigitalWalletsInPayloadNotEnabledArguments() {
        return Stream.of(
                arguments(Collections.singletonList(MULTI_MASTERCARD_ARTWORK_REFERENCE),
                        Arrays.asList(MULTI_MASTERCARD_ARTWORK_REFERENCE, MULTI_MASTERCARD_ARTWORK_REFERENCE2))
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

    protected CreateManagedCardModel.Builder getPrepaidCorporateManagedCardModel(){
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
                        corporateCurrency)
                .setCardholderMobileNumber(String.format("%s%s",
                        corporateDetails.getRootUser().getMobile().getCountryCode(),
                        corporateDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getDebitConsumerManagedCardModel(final String managedAccountId){
        return CreateManagedCardModel
                .DefaultCreateDebitManagedCardModel(consumerDebitManagedCardsProfileId,
                        managedAccountId)
                .setCardholderMobileNumber(String.format("%s%s",
                        consumerDetails.getRootUser().getMobile().getCountryCode(),
                        consumerDetails.getRootUser().getMobile().getNumber()));
    }

    protected void assertCommonResponseDetails(final ValidatableResponse response,
                                             final CreateManagedCardModel createManagedCardModel,
                                             final String managedCardProfileId,
                                             final IdentityType identityType,
                                             final boolean isWalletsEnabled,
                                             final boolean isPushProvisioningEnabled,
                                             final String artworkReference){
        response
                .body("id", notNullValue())
                .body("profileId", equalTo(managedCardProfileId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(createManagedCardModel.getTag()))
                .body("friendlyName", equalTo(createManagedCardModel.getFriendlyName()))
                .body("state.state", equalTo("ACTIVE"))
                .body("type", equalTo("VIRTUAL"))
                .body("cardBrand", equalTo("MASTERCARD"))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", equalTo("522093"))
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(createManagedCardModel.getNameOnCard()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("cardLevelClassification", equalTo(identityType.toString()))
                .body("expiryPeriodMonths", equalTo(36))
                .body("renewalType", equalTo("NO_RENEW"))
                .body("creationTimestamp", notNullValue())
                .body("cardholderMobileNumber", equalTo(createManagedCardModel.getCardholderMobileNumber()))
                .body("billingAddress.addressLine1", equalTo(createManagedCardModel.getBillingAddress().getAddressLine1()))
                .body("billingAddress.addressLine2", equalTo(createManagedCardModel.getBillingAddress().getAddressLine2()))
                .body("billingAddress.city", equalTo(createManagedCardModel.getBillingAddress().getCity()))
                .body("billingAddress.postCode", equalTo(createManagedCardModel.getBillingAddress().getPostCode()))
                .body("billingAddress.state", equalTo(createManagedCardModel.getBillingAddress().getState()))
                .body("billingAddress.country", equalTo(createManagedCardModel.getBillingAddress().getCountry()))
                .body("digitalWallets.walletsEnabled", equalTo(isWalletsEnabled))
                .body("digitalWallets.pushProvisioningEnabled", equalTo(isPushProvisioningEnabled))
                .body("digitalWallets.artworkReference", equalTo(artworkReference))
                .body("mode", equalTo(createManagedCardModel.getMode()));
    }

    private static void consumerSetup() {
        consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = consumerDetails.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }

    private static void corporateSetup() {

        corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }
}
