package opc.junit.multi.managedcards;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CardBrand;
import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KycLevel;
import opc.enums.opc.ManagedCardMode;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.admin.UpdateKybModel;
import opc.models.admin.UpdateKycModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.ExternalDataModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.AddressModel;
import opc.services.admin.AdminService;
import opc.services.innovatornew.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class CreateManagedCardTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static CreateCorporateModel corporateDetails;
    private static CreateConsumerModel consumerDetails;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
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
                createManagedAccount(corporateManagedAccountsProfileId, currency.name(), corporateAuthenticationToken);

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
    public void CreateManagedCard_PrepaidRequiredOnly_Success() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setTag(null)
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_BillingAddressRequiredOnly_Success() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setBillingAddress(AddressModel.DefaultAddressModel()
                                .setAddressLine2(null)
                                .setState(null)
                                .build())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_BillingAddressLine1TooLong_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidCorporateManagedCardModel()
                .setBillingAddress(AddressModel.DefaultAddressModel()
                    .setAddressLine1(RandomStringUtils.randomAlphabetic(151))
                    .setAddressLine2(null)
                    .setState(null)
                    .build())
                .build();


        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.billingAddress.addressLine1: size must be between 1 and 150"));
    }

    @Test
    public void CreateManagedCard_BillingAddressLine2TooLong_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidCorporateManagedCardModel()
                .setBillingAddress(AddressModel.DefaultAddressModel()
                    .setAddressLine1(RandomStringUtils.randomAlphabetic(10))
                    .setAddressLine2(RandomStringUtils.randomAlphabetic(151))
                    .setState(null)
                    .build())
                .build();


        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.billingAddress.addressLine2: size must be between 0 and 150"));
    }

    @Test
    public void CreateManagedCard_BillingAddressLine1AndLine2TooLong_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidCorporateManagedCardModel()
                .setBillingAddress(AddressModel.DefaultAddressModel()
                    .setAddressLine1(RandomStringUtils.randomAlphabetic(151))
                    .setAddressLine2(RandomStringUtils.randomAlphabetic(151))
                    .setState(null)
                    .build())
                .build();


        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("_embedded.errors[0].message", equalTo("request.billingAddress.addressLine1: size must be between 1 and 150"))
            .body("_embedded.errors[1].message", equalTo("request.billingAddress.addressLine2: size must be between 0 and 150"));
    }

    @Test
    public void CreateManagedCard_LongBillingAddressLine1AndLine2_Success() {
        final String addressLine1 = RandomStringUtils.randomAlphabetic(90);
        final String addressLine2 = RandomStringUtils.randomAlphabetic(90);
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidCorporateManagedCardModel()
                .setBillingAddress(AddressModel.DefaultAddressModel()
                    .setAddressLine1(addressLine1)
                    .setAddressLine2(addressLine2)
                    .setState(null)
                    .build())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("billingAddress.addressLine1", equalTo(addressLine1))
            .body("billingAddress.addressLine2", equalTo(addressLine2));
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
                createManagedAccount(consumerManagedAccountsProfileId, currency.name(), consumerAuthenticationToken);

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
    public void CreateManagedCard_PrepaidCorporateUser_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, user.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_DebitCorporateUser_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final String managedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, user.getRight());

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId).build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, user.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, corporateDebitManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_UpdatePrepaidProfileToDebit_Success() {
        final String managedCardsProfile =
                InnovatorHelper.createNitecrestPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER);

        final CreateManagedCardModel createPrepaidManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(managedCardsProfile, consumerCurrency)
                        .setCardholderMobileNumber(String.format("%s%s",
                                consumerDetails.getRootUser().getMobile().getCountryCode(),
                                consumerDetails.getRootUser().getMobile().getNumber())).build();

        ManagedCardsService.createManagedCard(createPrepaidManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        InnovatorHelper.updateManagedCardsProfileToDebitMode(innovatorToken, programmeId, managedCardsProfile);

        ManagedCardsService.createManagedCard(createPrepaidManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_MODEL_CONSTRAINTS_VIOLATED"));

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createDebitManagedCardModel =
                CreateManagedCardModel.DefaultCreateDebitManagedCardModel(managedCardsProfile, managedAccountId)
                        .setCardholderMobileNumber(String.format("%s%s",
                                consumerDetails.getRootUser().getMobile().getCountryCode(),
                                consumerDetails.getRootUser().getMobile().getNumber())).build();

        ManagedCardsService.createManagedCard(createDebitManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_UpdateDebitProfileToPrepaid_Success() {
        final String managedCardsProfile =
                InnovatorHelper.createNitecrestDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER);

        final String managedAccountId = createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        final CreateManagedCardModel createDebitManagedCardModel =
                CreateManagedCardModel.DefaultCreateDebitManagedCardModel(managedCardsProfile, managedAccountId)
                        .setCardholderMobileNumber(String.format("%s%s",
                                consumerDetails.getRootUser().getMobile().getCountryCode(),
                                consumerDetails.getRootUser().getMobile().getNumber())).build();

        ManagedCardsService.createManagedCard(createDebitManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        InnovatorHelper.updateManagedCardsProfileToPrepaidMode(innovatorToken, programmeId, managedCardsProfile, IdentityType.CONSUMER);

        ManagedCardsService.createManagedCard(createDebitManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_MODEL_CONSTRAINTS_VIOLATED"));

        final CreateManagedCardModel createPrepaidManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(managedCardsProfile, consumerCurrency)
                        .setCardholderMobileNumber(String.format("%s%s",
                                consumerDetails.getRootUser().getMobile().getCountryCode(),
                                consumerDetails.getRootUser().getMobile().getNumber())).build();

        ManagedCardsService.createManagedCard(createPrepaidManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitCorporateUnderNonFpsEnabledTenant_Success(final Currency currency) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenantDetails.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        createCorporateModel.getBaseCurrency(), nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(nonFpsEnabledTenantDetails.getCorporateNitecrestEeaDebitManagedCardsProfileId(),
                                managedAccountId)
                        .setCardholderMobileNumber(String.format("%s%s",
                                createCorporateModel.getRootUser().getMobile().getCountryCode(),
                                createCorporateModel.getRootUser().getMobile().getNumber()))
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, nonFpsEnabledTenantDetails.getCorporateNitecrestEeaDebitManagedCardsProfileId(), IdentityType.CORPORATE);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitConsumerUnderNonFpsEnabledTenant_Success(final Currency currency) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsEnabledTenantDetails.getConsumersProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        createConsumerModel.getBaseCurrency(), nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(nonFpsEnabledTenantDetails.getConsumerNitecrestEeaDebitManagedCardsProfileId(),
                                managedAccountId)
                        .setCardholderMobileNumber(String.format("%s%s",
                                createConsumerModel.getRootUser().getMobile().getCountryCode(),
                                createConsumerModel.getRootUser().getMobile().getNumber()))
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"));

        assertCommonResponseDetails(response, createManagedCardModel, nonFpsEnabledTenantDetails.getConsumerNitecrestEeaDebitManagedCardsProfileId(), IdentityType.CONSUMER);
    }

    @Test
    public void CreateManagedCard_AccountNotActive_ParentManagedAccountNotActive() {

        final String managedAccountId =
                createPendingApprovalManagedAccount(corporateManagedAccountsProfileId, corporateAuthenticationToken).getLeft();

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    public void CreateManagedCard_SameIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response -> {

                response.then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

            assertCommonResponseDetails(response.then(), createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
        });

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    @DisplayName("CreateManagedCard_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void CreateManagedCard_SameIdempotencyRefDifferentPayload_PreconditionFailed() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final CreateManagedCardModel createManagedCardModel1 = getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.createManagedCard(createManagedCardModel1,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_DifferentIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)));
        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.empty()));

        responses.forEach(response -> {

            response.then()
                    .statusCode(SC_OK)
                    .body("currency", equalTo(createManagedCardModel.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0));

            assertCommonResponseDetails(response.then(), createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
        });

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedCard_DifferentIdempotencyRefDifferentPayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();
        final CreateManagedCardModel createManagedCardModel1 = getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final Map<Response, CreateManagedCardModel> responses = new HashMap<>();

        responses.put(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                createManagedCardModel);
        responses.put(ManagedCardsService.createManagedCard(createManagedCardModel1,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)),
                createManagedCardModel1);
        responses.put(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.empty()),
                createManagedCardModel);

        responses.forEach((key, value) -> {

            key.then()
                    .statusCode(SC_OK)
                    .body("currency", equalTo(createManagedCardModel.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0));

            assertCommonResponseDetails(key.then(), value, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
        });

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertNotEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertNotEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responseList.get(1).jsonPath().getString("id"), responseList.get(2).jsonPath().getString("id"));
        assertNotEquals(responseList.get(1).jsonPath().getString("creationTimestamp"), responseList.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedCard_LongIdempotencyRef_RequestTooLong() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);

        ManagedCardsService.createManagedCard(createManagedCardModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_REQUEST_TOO_LONG);
    }

    @Test
    public void CreateManagedCard_ExpiredIdempotencyRef_NewRequestSuccess() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        TimeUnit.SECONDS.sleep(18);

        responses.add(ManagedCardsService.createManagedCard(createManagedCardModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response -> {

            response.then()
                    .statusCode(SC_OK)
                    .body("currency", equalTo(createManagedCardModel.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0));

            assertCommonResponseDetails(response.then(), createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
        });

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("2",
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedCard_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel.Builder createManagedCardModel =
                getPrepaidCorporateManagedCardModel();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        ManagedCardsService.createManagedCard(createManagedCardModel.setProfileId("123").build(),
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_CONFLICT);

        ManagedCardsService.createManagedCard(createManagedCardModel.setProfileId(corporatePrepaidManagedCardsProfileId).build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        assertEquals("1",
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedCard_SameIdempotencyRefSamePayloadWithChange_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel().build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final Map<Response, State> responses = new HashMap<>();

        final Response initialResponse =
                ManagedCardsService.createManagedCard(createManagedCardModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference));

        responses.put(initialResponse, State.ACTIVE);

        ManagedCardsHelper.blockManagedCard(secretKey, initialResponse.jsonPath().getString("id"), authenticatedCorporate.getRight());

        responses.put(ManagedCardsService.createManagedCard(createManagedCardModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                State.BLOCKED);

        responses.forEach((key, value) -> {

            key.then()
                    .statusCode(SC_OK)
                    .body("currency", equalTo(createManagedCardModel.getCurrency()))
                    .body("balances.availableBalance", equalTo(0))
                    .body("balances.actualBalance", equalTo(0));

            assertCommonResponseDetails(key.then(), createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE, value);
        });

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                ManagedCardsService.getManagedCards(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void CreateManagedCard_CorporateNotVerified_OwnerIdentityNotVerified() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_CorporateEmailNotVerified_OwnerIdentityNotVerified(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_CorporateMobileNumberNotVerified_OwnerIdentityNotVerified(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_CorporateOnlyKYCApproved_OwnerIdentityNotVerified(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_STARTED", "INITIATED", "PENDING_REVIEW", "REJECTED"})
    public void CreateManagedCard_CorporateKYCNotApproved_OwnerIdentityNotVerified(final String kybStatus){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified(kybStatus)
                .build();

        AdminHelper.updateCorporateKyb(updateKybModel, authenticatedCorporate.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_CorporateGradualVerifyingInOneSession_Success(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);


//KYC: approved, email: false, mobile: false
        UpdateKybModel updateKybModelKycApproved = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();
        AdminHelper.updateCorporateKyb(updateKybModelKycApproved, authenticatedCorporate.getLeft(), adminToken);
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();
        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: false
        UpdateKybModel updateKybModelEmailVerified = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified("APPROVED")
                .build();
        AdminHelper.updateCorporateKyb(updateKybModelEmailVerified, authenticatedCorporate.getLeft(), adminToken);
        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: true
        UpdateKybModel updateKybModelFullVerified = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified("APPROVED")
                .build();
        AdminHelper.updateCorporateKyb(updateKybModelFullVerified, authenticatedCorporate.getLeft(), adminToken);
        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_ConsumerNotVerified_OwnerIdentityNotVerified() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_ConsumerEmailNotVerified_OwnerIdentityNotVerified(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(true)
                .setFullDueDiligence("APPROVED")
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_ConsumerMobileNumberNotVerified_OwnerIdentityNotVerified(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_ConsumerOnlyKYCApproved_OwnerIdentityNotVerified(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_STARTED", "INITIATED", "PENDING_REVIEW", "REJECTED"})
    public void CreateManagedCard_ConsumerKYCNotApproved_OwnerIdentityNotVerified(final String kycStatus){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(true)
                .setFullDueDiligence(kycStatus)
                .build();

        AdminHelper.updateConsumerKyc(updateKycModel, authenticatedConsumer.getLeft(), adminToken);

        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));
    }

    @Test
    public void CreateManagedCard_ConsumerGradualVerifyingInOneSession_Success(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

//KYC: approved, email: false, mobile: false
        UpdateKycModel updateKycModelKycApproved = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();
        AdminHelper.updateConsumerKyc(updateKycModelKycApproved, authenticatedConsumer.getLeft(), adminToken);
        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();
        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: false
        UpdateKycModel updateKycModelEmailVerified = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(false)
                .setFullDueDiligence("APPROVED")
                .build();
        AdminHelper.updateConsumerKyc(updateKycModelEmailVerified, authenticatedConsumer.getLeft(), adminToken);
        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

//KYC: approved, email: true, mobile: true
        UpdateKycModel updateKycModelFullVerified = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(true)
                .setFullDueDiligence("APPROVED")
                .build();
        AdminHelper.updateConsumerKyc(updateKycModelFullVerified, authenticatedConsumer.getLeft(), adminToken);
        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, authenticatedConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_InvalidApiKey_Unauthorised(){
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, "abc", corporateAuthenticationToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateManagedCard_NoApiKey_BadRequest(){
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, "", corporateAuthenticationToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateManagedCard_UnknownManagedCardProfileId_ProfileNotFound() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setProfileId(RandomStringUtils.randomNumeric(18))
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void CreateManagedCard_InvalidMobileNumber_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setCardholderMobileNumber(RandomStringUtils.randomNumeric(8))
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_PrepaidModeWithDebitManagedCardProfile_ModelConstraintsViolated() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setProfileId(corporateDebitManagedCardsProfileId)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_MODEL_CONSTRAINTS_VIOLATED"));
    }

    @Test
    public void CreateManagedCard_DebitModeWithPrepaidManagedCardProfile_ModelConstraintsViolated() {
        final String managedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel =
                getDebitCorporateManagedCardModel(managedAccountId)
                        .setProfileId(corporatePrepaidManagedCardsProfileId)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_MODEL_CONSTRAINTS_VIOLATED"));
    }

    @Test
    public void CreateManagedCard_CrossIdentityManagedCardProfileId_ModelConstraintsViolated() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setProfileId(corporatePrepaidManagedCardsProfileId)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PAYMENT_MODEL_CONSTRAINTS_VIOLATED"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedCard_NoManagedCardProfileId_BadRequest(final String managedCardProfileId) {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setProfileId(managedCardProfileId)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedCard_PrepaidNoCurrency_BadRequest(final String currency) {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setCurrency(currency)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EU", "EURO"})
    public void CreateManagedCard_PrepaidInvalidCurrency_BadRequest(final String currency) {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setCurrency(currency)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_PrepaidUnknownCurrency_CurrencyNotSupported() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidCorporateManagedCardModel()
                        .setCurrency("ABC")
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_NOT_SUPPORTED_BY_PROFILE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedCard_NoFriendlyName_BadRequest(final String friendlyName) {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setFriendlyName(friendlyName)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedCard_NoNameOnCard_BadRequest(final String nameOnCard) {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setNameOnCard(nameOnCard)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_NameOnCardTooLong_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidConsumerManagedCardModel()
                .setNameOnCard(String.format("%s", RandomStringUtils.randomAlphabetic(30)))
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.nameOnCard: size must be between 1 and 27"));
    }

    @Test
    public void CreateManagedCard_NameOnCardLine2_Success() {
        final String nameOnCardLine2 = String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5));
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidConsumerManagedCardModel()
                .setNameOnCardLine2(nameOnCardLine2)
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void CreateManagedCard_NameOnCardLine2TooLong_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
            getPrepaidConsumerManagedCardModel()
                .setNameOnCardLine2(String.format("%s", RandomStringUtils.randomAlphabetic(30)))
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.nameOnCardLine2: size must be between 0 and 27"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedCard_NoNameOnCardLine2_Success(final String nameOnCardLine2) {

        final CreateManagedCardModel createManagedCardModel =
            getPrepaidConsumerManagedCardModel()
                .setNameOnCardLine2(nameOnCardLine2)
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("nameOnCardLine2", nullValue());
    }

    @Test
    public void CreateManagedCard_NoBillingAddress_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setBillingAddress(null)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_NoMode_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setMode(null)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_UnknownMode_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setMode("UNKNOWN")
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_PrepaidWithParentManagedAccountIdNoCurrency_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setMode(ManagedCardMode.PREPAID_MODE.name())
                        .setCurrency(null)
                        .setParentManagedAccountId(RandomStringUtils.randomNumeric(18))
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_DebitWithCurrencyNoParentManagedAccountId_BadRequest() {
        final CreateManagedCardModel createManagedCardModel =
                getPrepaidConsumerManagedCardModel()
                        .setMode(ManagedCardMode.DEBIT_MODE.name())
                        .setCurrency(consumerCurrency)
                        .setParentManagedAccountId(null)
                        .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_DebitCardManagedAccountBlocked_ParentManagedAccountNotActive() {
        final String managedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedAccountId, secretKey, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    public void CreateManagedCard_DebitCardManagedAccountRemoved_ParentManagedAccountNotActive() {
        final String managedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, consumerAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(managedAccountId).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    public void CreateManagedCard_DebitCardUnknownManagedAccount_ParentManagedAccountNotFound() {
        final CreateManagedCardModel createManagedCardModel =
                getDebitConsumerManagedCardModel(RandomStringUtils.randomNumeric(18)).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void CreateManagedCard_DebitCardOtherIdentityManagedAccount_ParentManagedAccountNotFound() {
        final String otherIdentityManagedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitConsumerManagedCardModel(otherIdentityManagedAccountId).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PARENT_MANAGED_ACCOUNT_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreateManagedCard_DebitCardNoManagedAccount_BadRequest(final String managedAccountId) {
        final CreateManagedCardModel createManagedCardModel =
                getDebitConsumerManagedCardModel(RandomStringUtils.randomNumeric(18))
                        .setParentManagedAccountId(managedAccountId).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateManagedCard_BackofficeCorporateImpersonator_Forbidden() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE),
                Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedCard_BackofficeConsumerImpersonator_Forbidden() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidConsumerManagedCardModel().build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER),
                Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedCard_EmojiInFriendlyName_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setFriendlyName("\uD83D\uDE1C")
                .build();

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
    public void CreateManagedCard_DebitKycLevel1_IdentityKycLevelStepupRequired(final Currency currency) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final String managedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, currency.name(), consumer.getRight());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(consumerDebitManagedCardsProfileId,
                                managedAccountId)
                        .setCardholderMobileNumber(String.format("%s%s",
                                createConsumerModel.getRootUser().getMobile().getCountryCode(),
                                createConsumerModel.getRootUser().getMobile().getNumber())).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("IDENTITY_KYC_LEVEL_STEPUP_REQUIRED"));
    }

    @Test
    public void CreateManagedCard_PrepaidKycLevel1_Success() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId,
                                createConsumerModel.getBaseCurrency())
                        .setCardholderMobileNumber(String.format("%s%s",
                                createConsumerModel.getRootUser().getMobile().getCountryCode(),
                                createConsumerModel.getRootUser().getMobile().getNumber())).build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, consumer.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0));

        assertCommonResponseDetails(response, createManagedCardModel, consumerPrepaidManagedCardsProfileId, IdentityType.CONSUMER);
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @EnumSource(value = DefaultTimeoutDecision.class)
    public void CreateManagedCard_PrepaidCorporate_AuthForwardingDefaultTimeoutDecision_Success(final DefaultTimeoutDecision defaultTimeoutDecision) {

        authForwardingConfiguration(true, true);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getCorporatePrepaidAuthForwardingCardProfileModel(true, defaultTimeoutDecision.name());

        final String profileId = InnovatorService.createManagedCardsProfileV2(createManagedCardsProfile, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");


        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setProfileId(profileId)
                .setAuthForwardingDefaultTimeoutDecision(defaultTimeoutDecision.name())
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("currency", equalTo(createManagedCardModel.getCurrency()))
                        .body("balances.availableBalance", equalTo(0))
                        .body("balances.actualBalance", equalTo(0))
                        .body("authForwardingDefaultTimeoutDecision", equalTo(defaultTimeoutDecision.name()));

        assertCommonResponseDetails(response, createManagedCardModel, profileId, IdentityType.CORPORATE);
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @EnumSource(value = DefaultTimeoutDecision.class)
    public void CreateManagedCard_DebitCorporate_AuthForwardingDefaultTimeoutDecision_Success(final DefaultTimeoutDecision defaultTimeoutDecision) {

        String adminTenantImpersonationToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());
        AdminHelper.setManagedCardTokenisationEnabledProperty(innovatorId, CardBrand.MASTERCARD,
                true, true, adminTenantImpersonationToken);

        authForwardingConfiguration(true, true);

        final String managedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, Currency.GBP.name(), corporateAuthenticationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getCorporateDebitAuthForwardingCardProfileModel(true, defaultTimeoutDecision.name());

        final String profileId = InnovatorService.createManagedCardsProfileV2(createManagedCardsProfile, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId)
                .setProfileId(profileId)
                .setAuthForwardingDefaultTimeoutDecision(defaultTimeoutDecision.name())
                .build();

        final ValidatableResponse response =
                ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(managedAccountId))
                        .body("availableToSpend[0].value.amount", equalTo(0))
                        .body("availableToSpend[0].interval", equalTo("ALWAYS"))
                        .body("authForwardingDefaultTimeoutDecision", equalTo(defaultTimeoutDecision.name()));

        assertCommonResponseDetails(response, createManagedCardModel, profileId, IdentityType.CORPORATE);
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @MethodSource("authForwardingLevels")
    public void CreateManagedCard_PrepaidCorporate_AuthForwardingDefaultTimeoutDecision__LevelsDisabled_Conflict(final boolean enableInnovator,
                                                                                                                final boolean enableProgramme,
                                                                                                                final boolean enableCardProfile) {
        if (enableCardProfile)
                authForwardingConfiguration(true, true);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getCorporatePrepaidAuthForwardingCardProfileModel(enableCardProfile, DefaultTimeoutDecision.APPROVE.name());

        final String profileId = InnovatorService.createManagedCardsProfileV2(createManagedCardsProfile, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");

        authForwardingConfiguration(enableInnovator, enableProgramme);

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel()
                .setProfileId(profileId)
                .setAuthForwardingDefaultTimeoutDecision(DefaultTimeoutDecision.APPROVE.name())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AUTH_FORWARDING_NOT_ENABLED"));
    }

    @ParameterizedTest
    @Execution(ExecutionMode.SAME_THREAD)
    @MethodSource("authForwardingLevels")
    public void CreateManagedCard_DebitCorporate_AuthForwardingDefaultTimeoutDecision__LevelsDisabled_Conflict(final boolean enableInnovator,
                                                                                                              final boolean enableProgramme,
                                                                                                              final boolean enableCardProfile) {
        if (enableCardProfile)
                authForwardingConfiguration(true, true);

        final String managedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, Currency.GBP.name(), corporateAuthenticationToken);

        final CreateManagedCardsProfileV2Model createManagedCardsProfile =
                getCorporateDebitAuthForwardingCardProfileModel(enableCardProfile, DefaultTimeoutDecision.APPROVE.name());

        final String profileId = InnovatorService.createManagedCardsProfileV2(createManagedCardsProfile, innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");

        authForwardingConfiguration(enableInnovator, enableProgramme);

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId)
                .setProfileId(profileId)
                .setAuthForwardingDefaultTimeoutDecision(DefaultTimeoutDecision.APPROVE.name())
                .build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AUTH_FORWARDING_NOT_ENABLED"));
    }

    @Test
    public void CreateManagedCard_ExternalData_Success() {
        final String externalDataName = RandomStringUtils.randomAlphabetic(10);
        final String externalDataValue = RandomStringUtils.randomAlphabetic(10);

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("currency", equalTo(createManagedCardModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0))
                .body("externalData[0].name", equalTo(externalDataName))
                .body("externalData[0].value", equalTo(externalDataValue));

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_ExternalDataEmptyString_BadRequest() {

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name("").value("").build());

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("_embedded.errors[0].message", equalTo("request.externalData[0].name: must match \"^[a-zA-Z0-9 ]+$\""))
            .body("_embedded.errors[1].message", equalTo("request.externalData[0].name: size must be between 1 and 50"))
            .body("_embedded.errors[2].message", equalTo("request.externalData[0].value: must match \"^[a-zA-Z0-9 ]+$\""))
            .body("_embedded.errors[3].message", equalTo("request.externalData[0].value: size must be between 1 and 50"));
    }

    @Test
    public void CreateManagedCard_MultipleExternalData_Success() {
        List<ExternalDataModel> externalData = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ExternalDataModel element = ExternalDataModel.builder()
                .name(RandomStringUtils.randomAlphabetic(10))
                .value(RandomStringUtils.randomAlphabetic(10))
                .build();
            externalData.add(element);
        }

        externalData.sort(Comparator.comparing(ExternalDataModel::getName));

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("currency", equalTo(createManagedCardModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));


        for (ExternalDataModel element : externalData) {
            assertThat(response.extract().jsonPath().getList("externalData.name"), hasItem(element.getName()));
            assertThat(response.extract().jsonPath().getList("externalData.value"), hasItem(element.getValue()));
        }

        assertCommonResponseDetails(response, createManagedCardModel, corporatePrepaidManagedCardsProfileId, IdentityType.CORPORATE);
    }

    @Test
    public void CreateManagedCard_ExternalDataNameTooLong_BadRequest() {
        final String externalDataName = RandomStringUtils.randomAlphabetic(51);
        final String externalDataValue = RandomStringUtils.randomAlphabetic(10);

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.externalData[0].name: size must be between 1 and 50"));
    }

    @Test
    public void CreateManagedCard_ExternalDataValueTooLong_BadRequest() {
        final String externalDataName = RandomStringUtils.randomAlphabetic(10);
        final String externalDataValue = RandomStringUtils.randomAlphabetic(51);

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.externalData[0].value: size must be between 1 and 50"));
    }

    @Test
    public void CreateManagedCard_ExternalDataTooManyElements_BadRequest() {

        List<ExternalDataModel> externalData = new ArrayList<>();

        for (int i = 0; i < 11; i++) {
            ExternalDataModel element = ExternalDataModel.builder()
                .name(RandomStringUtils.randomAlphabetic(10))
                .value(RandomStringUtils.randomAlphabetic(10))
                .build();
            externalData.add(element);
        }

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("message", equalTo("request.externalData: size must be between 0 and 10"));
    }

    @Test
    public void CreateManagedCard_ExternalDataInvalidCharacters_BadRequest() {
        final String externalDataName = "!.#$";
        final String externalDataValue = "!.#$";

        final List<ExternalDataModel> externalData = List.of(ExternalDataModel.builder().name(externalDataName).value(externalDataValue).build());

        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModelWithExternalData(externalData).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("_embedded.errors[0].message", equalTo("request.externalData[0].name: must match \"^[a-zA-Z0-9 ]+$\""))
            .body("_embedded.errors[1].message", equalTo("request.externalData[0].value: must match \"^[a-zA-Z0-9 ]+$\""));
    }

    @Test
    public void CreateManagedCard_PrepaidWithRenewal_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().setRenewalType("RENEW").build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("currency", equalTo(createManagedCardModel.getCurrency()))
            .body("renewalType", equalTo("RENEW"))
            .body("renewalTimestamp", notNullValue())
            .body("balances.availableBalance", equalTo(0))
            .body("balances.actualBalance", equalTo(0));
    }

    @Test
    public void CreateManagedCard_PrepaidNoRenewal_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().setRenewalType("NO_RENEW").build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("currency", equalTo(createManagedCardModel.getCurrency()))
            .body("renewalType", equalTo("NO_RENEW"))
            .body("balances.availableBalance", equalTo(0))
            .body("balances.actualBalance", equalTo(0));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitRenewal_Success(final Currency currency) {

        final String managedAccountId =
            createManagedAccount(corporateManagedAccountsProfileId, currency.name(), corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId).setRenewalType("RENEW").build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("parentManagedAccountId", equalTo(managedAccountId))
                .body("renewalType", equalTo("RENEW"))
                .body("renewalTimestamp", notNullValue())
                .body("availableToSpend[0].value.amount", equalTo(0))
                .body("availableToSpend[0].interval", equalTo("ALWAYS"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedCard_DebitNoRenewal_Success(final Currency currency) {

        final String managedAccountId =
            createManagedAccount(corporateManagedAccountsProfileId, currency.name(), corporateAuthenticationToken);

        final CreateManagedCardModel createManagedCardModel = getDebitCorporateManagedCardModel(managedAccountId).setRenewalType("NO_RENEW").build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("parentManagedAccountId", equalTo(managedAccountId))
            .body("renewalType", equalTo("NO_RENEW"))
            .body("availableToSpend[0].value.amount", equalTo(0))
            .body("availableToSpend[0].interval", equalTo("ALWAYS"));
    }

    @Test
    public void CreateManagedCard_ExpireRenewableCard_Conflict() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().setRenewalType("RENEW").build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final String managedCardId = response.extract().jsonPath().get("id");

        SimulatorService.simulateExpire(managedCardId, secretKey)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("INSTRUMENT_NOT_EXPIRABLE"));
    }

    @Test
    public void CreateManagedCard_RenewNonRenewableCard_Conflict() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().setRenewalType("NO_RENEW").build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final String managedCardId = response.extract().jsonPath().get("id");

        SimulatorService.simulateRenew(managedCardId, secretKey)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("INSTRUMENT_NOT_RENEWABLE"));
    }

    @Test
    public void CreateManagedCard_RenewVirtualCard_Success() {
        final CreateManagedCardModel createManagedCardModel = getPrepaidCorporateManagedCardModel().setRenewalType("RENEW").build();

        final ValidatableResponse response =
            ManagedCardsService.createManagedCard(createManagedCardModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final String managedCardId = response.extract().jsonPath().get("id");
        final String cvv = response.extract().jsonPath().get("cvv.value");
        final Long renewalTimestamp = response.extract().jsonPath().get("renewalTimestamp");

        SimulatorService.simulateRenew(managedCardId, secretKey).then()
            .statusCode(SC_NO_CONTENT);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, corporateAuthenticationToken)
            .then()
            .statusCode(SC_OK)
            .body("cvv.value", is(not(equalTo(cvv))))
            .body("renewalTimestamp", is(greaterThan((renewalTimestamp))));
    }

    private CreateManagedCardModel.Builder getPrepaidCorporateManagedCardModel(){
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
                        corporateCurrency)
                .setCardholderMobileNumber(String.format("%s%s",
                        corporateDetails.getRootUser().getMobile().getCountryCode(),
                        corporateDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getPrepaidCorporateManagedCardModelWithExternalData(List<ExternalDataModel> externalData){
        return CreateManagedCardModel
            .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
                corporateCurrency)
            .setCardholderMobileNumber(String.format("%s%s",
                corporateDetails.getRootUser().getMobile().getCountryCode(),
                corporateDetails.getRootUser().getMobile().getNumber()))
            .setExternalData(externalData);
    }

    private CreateManagedCardModel.Builder getDebitCorporateManagedCardModel(final String managedAccountId){
        return CreateManagedCardModel
                .DefaultCreateDebitManagedCardModel(corporateDebitManagedCardsProfileId,
                        managedAccountId)
                .setCardholderMobileNumber(String.format("%s%s",
                        corporateDetails.getRootUser().getMobile().getCountryCode(),
                        corporateDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getPrepaidConsumerManagedCardModel(){
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardsProfileId,
                        consumerCurrency)
                .setCardholderMobileNumber(String.format("%s%s",
                        consumerDetails.getRootUser().getMobile().getCountryCode(),
                        consumerDetails.getRootUser().getMobile().getNumber()));
    }

    private CreateManagedCardModel.Builder getDebitConsumerManagedCardModel(final String managedAccountId){
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
                                             final IdentityType identityType,
                                             final State state){
        response
                .body("id", notNullValue())
                .body("profileId", equalTo(managedCardProfileId))
                .body("externalHandle", notNullValue())
                .body("tag", equalTo(createManagedCardModel.getTag()))
                .body("friendlyName", equalTo(createManagedCardModel.getFriendlyName()))
                .body("state.state", equalTo(state.name()))
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

    private void assertCommonResponseDetails(final ValidatableResponse response,
                                             final CreateManagedCardModel createManagedCardModel,
                                             final String managedCardProfileId,
                                             final IdentityType identityType){
        assertCommonResponseDetails(response, createManagedCardModel, managedCardProfileId, identityType, State.ACTIVE);
    }

    private static void consumerSetup() {
        consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = consumerDetails.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = corporateDetails.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
