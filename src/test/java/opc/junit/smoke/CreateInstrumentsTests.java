package opc.junit.smoke;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

public class CreateInstrumentsTests extends BaseSmokeSetup {
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static CreateCorporateModel corporateDetails;
    private static CreateConsumerModel consumerDetails;
    private static String existingConsumerAuthenticationToken;
    private static String existingCorporateAuthenticationToken;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();

        existingConsumerAuthenticationToken = getExistingConsumerDetails().getLeft();
        existingCorporateAuthenticationToken = getExistingCorporateDetails().getLeft();
    }

    @Test
    public void CreateManagedCard_PrepaidCorporate_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitCorporate_Success(final Currency currency) {
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId).build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, corporateDebitManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_PrepaidConsumer_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, consumerPrepaidManagedCardsProfileId, IdentityType.CONSUMER);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitConsumer_Success(final Currency currency) {
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountsProfileId, currency.name(), secretKey, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId).build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, consumerDebitManagedCardsProfileId, IdentityType.CONSUMER);
    }

    @Test
    public void CreateManagedCard_PrepaidExistingConsumer_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, existingConsumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, consumerPrepaidManagedCardsProfileId, IdentityType.CONSUMER);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitExistingConsumer_Success(final Currency currency) {
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountsProfileId, currency.name(), secretKey, existingConsumerAuthenticationToken);
        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId).build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, existingConsumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, consumerDebitManagedCardsProfileId, IdentityType.CONSUMER);
    }

    @Test
    public void CreateManagedCard_PrepaidExistingCorporate_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, existingCorporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitExistingCorporate_Success(final Currency currency) {
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, existingCorporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId).build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, existingCorporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, corporateDebitManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_Corporate_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_ExistingCorporate_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, existingCorporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(corporateManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_Consumer_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_ExistingConsumer_Success(final Currency currency) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId,
                                currency)
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, existingConsumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(consumerManagedAccountProfileId))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("state.state", equalTo(currency.equals(Currency.GBP) ? "BLOCKED" : "ACTIVE"))
                .body("bankAccountDetails", nullValue())
                .body("creationTimestamp", notNullValue());
    }

    private CreateManagedCardModel.Builder getPrepaidCorporateManagedCardModel() {
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
                        corporateCurrency)
                .setCardholderMobileNumber(String.format("%s%s",
                        corporateDetails.getRootUser().getMobile().getCountryCode(),
                        corporateDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getDebitCorporateManagedCardModel(final String managedAccountId) {
        return CreateManagedCardModel
                .DefaultCreateDebitManagedCardModel(corporateDebitManagedCardsProfileId,
                        managedAccountId)
                .setCardholderMobileNumber(String.format("%s%s",
                        corporateDetails.getRootUser().getMobile().getCountryCode(),
                        corporateDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getPrepaidConsumerManagedCardModel() {
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId,
                        consumerCurrency)
                .setCardholderMobileNumber(String.format("%s%s",
                        consumerDetails.getRootUser().getMobile().getCountryCode(),
                        consumerDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getDebitConsumerManagedCardModel(final String managedAccountId) {
        return CreateManagedCardModel
                .DefaultCreateDebitManagedCardModel(consumerDebitManagedCardsProfileId,
                        managedAccountId)
                .setCardholderMobileNumber(String.format("%s%s",
                        consumerDetails.getRootUser().getMobile().getCountryCode(),
                        consumerDetails.getRootUser().getMobile().getNumber()));
    }

    private void assertCommonResponseDetails(final ValidatableResponse response,
                                             final CreateManagedCardModel createManagedCardModel,
                                             final String managedCardProfileId,
                                             final IdentityType identityType) {
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
                .body("digitalWallets.walletsEnabled", equalTo(false))
                .body("digitalWallets.artworkReference", equalTo(null))
                .body("mode", equalTo(createManagedCardModel.getMode()));
    }

    private static void consumerSetup() {
        consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerDetails, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = consumerDetails.getBaseCurrency();
    }

    private static void corporateSetup() {
        corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateDetails, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();
    }

}
