package opc.junit.backoffice.fees;

import io.restassured.response.Response;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.innovator.FeeDetailsModel;
import opc.models.innovator.FeeModel;
import opc.models.innovator.FeeValuesModel;
import opc.models.innovator.UpdateConsumerProfileModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.FeeSourceModel;
import opc.models.shared.FeesChargeModel;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiBackofficeTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(MultiBackofficeTags.MULTI_BACKOFFICE_IDENTITIES)
public class ChargeFeeTests extends BaseIdentitySetup{
    private final static long MANAGED_ACCOUNT_DEPOSIT_AMOUNT = 10000L;
    private final static long MANAGED_CARD_DEPOSIT_AMOUNT = 300L;

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String consumerCurrency;
    private static String consumerImpersonateToken;
    private static String corporateCurrency;
    private static String corporateImpersonateToken;


    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void ChargeFee_ConsumerManagedAccount_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, consumerAuthenticationToken, consumerCurrency);

        final int chargeFee = TestHelper.getFees(consumerCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("profileId", equalTo(consumerProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_accounts"))
                .body("source.id", equalTo(managedAccountId))
                .body("availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());


        final int depositFee = TestHelper.getFees(consumerCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - getManagedAccountFees(consumerCurrency));
        assertManagedAccountBalance(managedAccountId, expectedBalance, consumerAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT)))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - depositFee)))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(depositFee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ChargeFee_ConsumerPrepaidCard_Success() {

        final String managedCardId = createPrepaidCard(consumerPrepaidManagedCardsProfileId ,consumerAuthenticationToken, consumerCurrency);
        simulateManagedCardDeposit(consumerManagedAccountProfileId, managedCardId, consumerAuthenticationToken, consumerCurrency);

        final int chargeFee = TestHelper.getFees(consumerCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("profileId", equalTo(consumerProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_cards"))
                .body("source.id", equalTo(managedCardId))
                .body("availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());


        final int expectedBalance = (int) (MANAGED_CARD_DEPOSIT_AMOUNT - getManagedCardFees(consumerCurrency));
        assertManagedCardBalance(managedCardId, expectedBalance, consumerAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, consumerAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int) MANAGED_CARD_DEPOSIT_AMOUNT))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) MANAGED_CARD_DEPOSIT_AMOUNT))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ChargeFee_ConsumerDebitCard_Success() {

        final String managedAccountId = createManagedAccount();
        final String managedCardId = createDebitCard(consumerDebitManagedCardsProfileId, managedAccountId, consumerAuthenticationToken);
        simulateManagedAccountDeposit(managedAccountId, consumerAuthenticationToken, consumerCurrency);

        final int chargeFee = TestHelper.getFees(consumerCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("profileId", equalTo(consumerProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_cards"))
                .body("source.id", equalTo(managedCardId))
                .body("availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(0))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        final int depositFee = TestHelper.getFees(consumerCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - getManagedAccountFees(consumerCurrency));
        assertManagedAccountBalance(managedAccountId, expectedBalance, consumerAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCardId))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT)))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - depositFee)))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(depositFee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, consumerAuthenticationToken, 1)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void ChargeFee_InvalidApiKey_Unauthorised() {

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, "abc", consumerImpersonateToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ChargeFee_NoApiKey_BadRequest() {

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, "", corporateImpersonateToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("REQUIRED"))
                .body("validationErrors[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void ChargeFee_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }


    @Test
    public void ChargeFee_NoFunds_InsufficientFunds() {
        final String managedAccountId = createManagedAccount();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void ChargeFee_DifferentInnovatorManagedAccount_UnresolvedInstrument() {

        final Pair<String, String> otherInnovatorConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(nonFpsTenant.getConsumersProfileId(), nonFpsTenant.getSecretKey());
        final String newInnovatorManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsTenant.getConsumerPayneticsEeaManagedAccountsProfileId(), consumerCurrency, nonFpsTenant.getSecretKey(), otherInnovatorConsumer.getRight());

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", newInnovatorManagedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeFee_InvalidSourceType_BadRequest() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("unknown", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_TYPE_OR_VALUE"))
                .body("validationErrors[0].fieldName", equalTo("source"))
                .body("validationErrors[1].fieldName", equalTo("type"))
                .body("validationErrors[1].error", equalTo("INVALID_TYPE_OR_VALUE"));
    }

    @Test
    public void ChargeFee_UnknownManagedAccountId_Conflict() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ChargeFee_InvalidManagedAccountId_BadRequest(String id) {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", id));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error",anyOf(is("HAS_TEXT"), is("REGEX")))
                .body("validationErrors[0].fieldName", equalTo("source.id"));
    }

    @Test
    public void ChargeFee_ConsumerToken_Forbidden() {

        final String managedAccountId = createManagedAccount();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ChargeFee_ConsumerSameIdempotencyRefSamePayload_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, consumerAuthenticationToken, consumerCurrency);

        final int chargeFee = TestHelper.getFees(consumerCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.of(idempotencyReference)));
        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.of(idempotencyReference)));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("transactionId.type", equalTo("CHARGE_FEE"))
                        .body("transactionId.id", notNullValue())
                        .body("profileId", equalTo(consumerProfileId))
                        .body("feeType", equalTo(feesChargeModel.getFeeType()))
                        .body("source.type", equalTo("managed_accounts"))
                        .body("source.id", equalTo(managedAccountId))
                        .body("availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                        .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        assertEquals(responses.get(0).jsonPath().getString("transactionId.id"), responses.get(1).jsonPath().getString("transactionId.id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
    }

    @Test
    @DisplayName("ChargeFee_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void ChargeFee_SameIdempotencyRefDifferentPayload_PreconditionFailed() {
        setCustomFee();

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, consumerAuthenticationToken, consumerCurrency);

        final int chargeFee = TestHelper.getFees(consumerCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final FeesChargeModel feesChargeModel1 =
                new FeesChargeModel("CUSTOM_FEE",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("transactionId.id", notNullValue())
                .body("profileId", equalTo(consumerProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_accounts"))
                .body("source.id", equalTo(managedAccountId))
                .body("availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        BackofficeMultiService.chargeFee(feesChargeModel1, secretKey, consumerImpersonateToken, Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validationErrors[0].error", equalTo("INVALID_REQUEST"));
    }

    @Test
    public void ChargeFee_ConsumerDifferentIdempotencyRef_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, consumerAuthenticationToken, consumerCurrency);

        final int chargeFee = TestHelper.getFees(consumerCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.of(idempotencyReference)));
        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.of(idempotencyReference1)));
        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty()));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("transactionId.type", equalTo("CHARGE_FEE"))
                        .body("transactionId.id", notNullValue())
                        .body("profileId", equalTo(consumerProfileId))
                        .body("feeType", equalTo(feesChargeModel.getFeeType()))
                        .body("source.type", equalTo("managed_accounts"))
                        .body("source.id", equalTo(managedAccountId))
                        .body("availableBalanceAdjustment.currency", equalTo(consumerCurrency))
                        .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        assertNotEquals(responses.get(0).jsonPath().getString("transactionId.id"), responses.get(1).jsonPath().getString("transactionId.id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("transactionId.id"), responses.get(2).jsonPath().getString("transactionId.id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
    }

    @Test
    public void ChargeFee_CorporateManagedAccount_Success() {

        final String managedAccountId = createCorporateManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, corporateAuthenticationToken, corporateCurrency);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("profileId", equalTo(corporateProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_accounts"))
                .body("source.id", equalTo(managedAccountId))
                .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        final int depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

        final int expectedBalance = (int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - getManagedAccountFees(corporateCurrency));
        assertManagedAccountBalance(managedAccountId, expectedBalance, corporateAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - depositFee)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(depositFee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ChargeFee_CorporatePrepaidCard_Success() {

        final String managedCardId = createPrepaidCard(corporatePrepaidManagedCardsProfileId,corporateAuthenticationToken, corporateCurrency);
        simulateManagedCardDeposit(corporateManagedAccountProfileId, managedCardId, corporateAuthenticationToken, corporateCurrency);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("profileId", equalTo(corporateProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_cards"))
                .body("source.id", equalTo(managedCardId))
                .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());


        final int expectedBalance = (int) (MANAGED_CARD_DEPOSIT_AMOUNT - getManagedCardFees(corporateCurrency));
        assertManagedCardBalance(managedCardId, expectedBalance, corporateAuthenticationToken);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int) MANAGED_CARD_DEPOSIT_AMOUNT))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) MANAGED_CARD_DEPOSIT_AMOUNT))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ChargeFee_CorporateDebitCard_Success() {

        final String managedAccountId = createCorporateManagedAccount();
        final String managedCardId = createDebitCard(corporateDebitManagedCardsProfileId ,managedAccountId, corporateAuthenticationToken);
        simulateManagedAccountDeposit(managedAccountId, corporateAuthenticationToken, corporateCurrency);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("profileId", equalTo(corporateProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_cards"))
                .body("source.id", equalTo(managedCardId))
                .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(0))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        final int depositFee = TestHelper.getFees(corporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

        final int expectedBalance = (int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - getManagedAccountFees(corporateCurrency));
        assertManagedAccountBalance(managedAccountId, expectedBalance, corporateAuthenticationToken);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCardId))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - depositFee)))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(depositFee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, corporateAuthenticationToken, 1)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("CHARGE_FEE"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact(chargeFee)))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.chargeFeeType", equalTo("PRINTED_CARD_ACCOUNT_STATEMENT"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void ChargeFee_RootUserLoggedOut_Unauthorised() {

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, corporateImpersonateToken, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ChargeFee_InvalidFeeType_UnresolvedFeeType() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("unknown",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_FEE_TYPE"));
    }

    @Test
    public void ChargeFee_UnknownManagedAccountId_UnresolvedInstrument() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeFee_CorporateToken_Forbidden() {

        final String managedAccountId = createCorporateManagedAccount();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ChargeFee_CorporateSameIdempotencyRefSamePayload_Success() {

        final String managedAccountId = createCorporateManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, corporateAuthenticationToken, corporateCurrency);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.of(idempotencyReference)));
        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.of(idempotencyReference)));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("transactionId.type", equalTo("CHARGE_FEE"))
                        .body("transactionId.id", notNullValue())
                        .body("profileId", equalTo(corporateProfileId))
                        .body("feeType", equalTo(feesChargeModel.getFeeType()))
                        .body("source.type", equalTo("managed_accounts"))
                        .body("source.id", equalTo(managedAccountId))
                        .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                        .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        assertEquals(responses.get(0).jsonPath().getString("transactionId.id"), responses.get(1).jsonPath().getString("transactionId.id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
    }


    @Test
    public void ChargeFee_CorporateDifferentIdempotencyRef_Success() {

        final String managedAccountId = createCorporateManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, corporateAuthenticationToken, corporateCurrency);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.of(idempotencyReference)));
        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.of(idempotencyReference1)));
        responses.add(BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty()));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("transactionId.type", equalTo("CHARGE_FEE"))
                        .body("transactionId.id", notNullValue())
                        .body("profileId", equalTo(corporateProfileId))
                        .body("feeType", equalTo(feesChargeModel.getFeeType()))
                        .body("source.type", equalTo("managed_accounts"))
                        .body("source.id", equalTo(managedAccountId))
                        .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                        .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        assertNotEquals(responses.get(0).jsonPath().getString("transactionId.id"), responses.get(1).jsonPath().getString("transactionId.id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("transactionId.id"), responses.get(2).jsonPath().getString("transactionId.id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));

    }

    @Test
    public void ChargeFee_CorporateManagedAccountConsumerImpersonate_Conflict(){
        final String managedAccountId = createCorporateManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, corporateAuthenticationToken, corporateCurrency);

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeFee_ConsumerManagedAccountCorporateImpersonate_Conflict(){
        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId, consumerAuthenticationToken, consumerCurrency);

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    private long getManagedAccountFees(String currency) {
        final Map<FeeType, CurrencyAmount> fees =
                TestHelper.getFees(currency).entrySet().stream()
                        .filter(x -> x.getKey().equals(FeeType.DEPOSIT_FEE) || x.getKey().equals(FeeType.CHARGE_FEE))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fees.values().stream().mapToLong(CurrencyAmount::getAmount).sum();
    }

    private long getManagedCardFees(String currency) {
        final Map<FeeType, CurrencyAmount> fees =
                TestHelper.getFees(currency).entrySet().stream()
                        .filter(x -> x.getKey().equals(FeeType.CHARGE_FEE))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fees.values().stream().mapToLong(CurrencyAmount::getAmount).sum();
    }

    private void assertManagedAccountBalance(final String managedAccountId, final int expectedBalance, String token) {
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(expectedBalance))
                .body("balances.actualBalance", equalTo(expectedBalance));
    }

    private void assertManagedCardBalance(final String managedCardId, final int expectedBalance, String token) {
        ManagedCardsService.getManagedCard(secretKey, managedCardId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(expectedBalance))
                .body("balances.actualBalance", equalTo(expectedBalance));
    }

    private void simulateManagedAccountDeposit(final String managedAccountId, String token, String currency) {
        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency,
                MANAGED_ACCOUNT_DEPOSIT_AMOUNT, secretKey, token);
    }

    private void simulateManagedCardDeposit(final String managedAccountProfileId, final String managedCardId, String authToken, String currency) {
        TestHelper.simulateManagedAccountDepositAndTransferToCard(managedAccountProfileId, transfersProfileId,
                managedCardId, currency, MANAGED_CARD_DEPOSIT_AMOUNT, secretKey, authToken);
    }

    private String createManagedAccount() {
        return ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                consumerCurrency,
                secretKey,
                consumerAuthenticationToken);
    }
    private String createCorporateManagedAccount() {
        return ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                corporateCurrency,
                secretKey,
                corporateAuthenticationToken);
    }

    private String createPrepaidCard(String cardProfileId, String token, String currency) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(cardProfileId, currency).build();

        return ManagedCardsHelper
                .createManagedCard(createManagedCardModel, secretKey, token);
    }

    private String createDebitCard(String cardProfileId, final String managedAccountId, String token) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(cardProfileId, managedAccountId).build();

        return ManagedCardsHelper
                .createManagedCard(createManagedCardModel, secretKey, token);
    }

    private void setCustomFee(){
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail,innovatorPassword);
        final UpdateConsumerProfileModel updateConsumerProfileModel = UpdateConsumerProfileModel.builder()
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("CUSTOM_FEE")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L)))))))).build();

        InnovatorHelper.updateConsumerProfile(updateConsumerProfileModel, innovatorToken, programmeId, consumerProfileId);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        corporateImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(corporateId, IdentityType.CORPORATE, secretKey);
    }
    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        consumerImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(consumerId, IdentityType.CONSUMER, secretKey);
    }
}
