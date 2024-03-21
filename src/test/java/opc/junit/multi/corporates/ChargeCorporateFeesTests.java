package opc.junit.multi.corporates;

import io.restassured.response.Response;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.innovator.FeeDetailsModel;
import opc.models.innovator.FeeModel;
import opc.models.innovator.FeeValuesModel;
import opc.models.innovator.UpdateCorporateProfileModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.FeeSourceModel;
import opc.models.shared.FeesChargeModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ChargeCorporateFeesTests extends BaseCorporatesSetup {

    private final static long MANAGED_ACCOUNT_DEPOSIT_AMOUNT = 10000L;
    private final static long MANAGED_CARD_DEPOSIT_AMOUNT = 300L;

    private static String authenticationToken;
    private static String corporateCurrency;
    private static String corporateId;

    @BeforeAll
    public static void Setup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        authenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
        corporateId = authenticatedCorporate.getLeft();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }

    @Test
    public void ChargeCorporateFee_ManagedAccount_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
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

        final int expectedBalance = (int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - getManagedAccountFees());
        assertManagedAccountBalance(managedAccountId, expectedBalance);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken, 2)
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
    public void ChargeCorporateFee_PrepaidCard_Success() {

        final String managedCardId = createPrepaidCard();
        simulateManagedCardDeposit(managedCardId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
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


        final int expectedBalance = (int) (MANAGED_CARD_DEPOSIT_AMOUNT - getManagedCardFees());
        assertManagedCardBalance(managedCardId, expectedBalance);

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, authenticationToken, 2)
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
    public void ChargeCorporateFee_DebitCard_Success() {

        final String managedAccountId = createManagedAccount();
        final String managedCardId = createDebitCard(managedAccountId);
        simulateManagedAccountDeposit(managedAccountId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_cards", managedCardId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
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

        final int expectedBalance = (int) (MANAGED_ACCOUNT_DEPOSIT_AMOUNT - getManagedAccountFees());
        assertManagedAccountBalance(managedAccountId, expectedBalance);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken, 2)
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

        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, authenticationToken, 1)
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
    public void ChargeCorporateFee_InvalidApiKey_Unauthorised() {

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, "abc", authenticationToken, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ChargeCorporateFee_NoApiKey_BadRequest() {

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, "", authenticationToken, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeCorporateFee_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ChargeCorporateFee_RootUserLoggedOut_Unauthorised() {

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ChargeCorporateFee_NoFunds_InsufficientFunds() {
        final String managedAccountId = createManagedAccount();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void ChargeCorporateFee_DifferentInnovatorManagedAccount_UnresolvedInstrument() {

        final Pair<String, String> otherInnovatorCorporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(nonFpsTenant.getCorporatesProfileId(), nonFpsTenant.getSecretKey());
        final String newInnovatorManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), corporateCurrency, nonFpsTenant.getSecretKey(), otherInnovatorCorporate.getRight());

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", newInnovatorManagedAccountId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @Test
    public void ChargeCorporateFee_InvalidSourceType_BadRequest() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("unknown", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeCorporateFee_InvalidFeeType_UnresolvedFeeType() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("unknown",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_FEE_TYPE"));
    }

    @Test
    public void ChargeCorporateFee_UnknownManagedAccountId_UnresolvedInstrument() {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_INSTRUMENT"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"", "abc"})
    public void ChargeCorporateFee_InvalidManagedAccountId_BadRequest(String id) {
        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", id));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeCorporateFee_BackofficeImpersonator_Forbidden() {

        final String managedAccountId = createManagedAccount();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ChargeCorporateFee_SameIdempotencyRefSamePayload_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));

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

        assertEquals(1, ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken).jsonPath().getList("entry.transactionId.type").stream().filter(x -> x.equals("CHARGE_FEE")).count());
    }

    @Test
    @DisplayName("ChargeCorporateFee_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void ChargeCorporateFee_SameIdempotencyRefDifferentPayload_PreconditionFailed() {

        setCustomFee();

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final FeesChargeModel feesChargeModel1 =
                new FeesChargeModel("CUSTOM_FEE",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
                .then()
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
                .body("creationTimestamp", notNullValue());

        CorporatesService.chargeCorporateFee(feesChargeModel1, secretKey, authenticationToken, Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ChargeCorporateFee_DifferentIdempotencyRefSamePayload_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));
        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference1)));
        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty()));

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

        assertEquals(3, ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken).jsonPath().getList("entry.transactionId.type").stream().filter(x -> x.equals("CHARGE_FEE")).count());
    }

    @Test
    public void ChargeCorporateFee_DifferentIdempotencyRefDifferentPayload_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final String managedAccountId1 = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId1);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final FeesChargeModel feesChargeModel1 =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId1));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final Map<Response, FeesChargeModel> responses = new HashMap<>();

        responses.put(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference)),
                feesChargeModel);
        responses.put(CorporatesService.chargeCorporateFee(feesChargeModel1, secretKey, authenticationToken, Optional.of(idempotencyReference1)),
                feesChargeModel1);
        responses.put(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.empty()),
                feesChargeModel);

        responses.forEach((key, value) ->

                key.then()
                        .statusCode(SC_OK)
                        .body("transactionId.type", equalTo("CHARGE_FEE"))
                        .body("transactionId.id", notNullValue())
                        .body("profileId", equalTo(corporateProfileId))
                        .body("feeType", equalTo(value.getFeeType()))
                        .body("source.type", equalTo("managed_accounts"))
                        .body("source.id", equalTo(value.getSource().getId()))
                        .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                        .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertNotEquals(responseList.get(0).jsonPath().getString("transactionId.id"), responseList.get(1).jsonPath().getString("transactionId.id"));
        assertNotEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responseList.get(1).jsonPath().getString("transactionId.id"), responseList.get(2).jsonPath().getString("transactionId.id"));
        assertNotEquals(responseList.get(1).jsonPath().getString("creationTimestamp"), responseList.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals(2, ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken).jsonPath().getList("entry.transactionId.type").stream().filter(x -> x.equals("CHARGE_FEE")).count());
        assertEquals(1, ManagedAccountsHelper.getManagedAccountStatement(managedAccountId1, secretKey, authenticationToken).jsonPath().getList("entry.transactionId.type").stream().filter(x -> x.equals("CHARGE_FEE")).count());
    }

    @Test
    public void ChargeCorporateFee_LongIdempotencyRef_RequestTooLong() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_REQUEST_TOO_LONG);
    }

    @Test
    public void ChargeCorporateFee_ExpiredIdempotencyRef_NewRequestSuccess() throws InterruptedException {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final int chargeFee = TestHelper.getFees(corporateCurrency).get(FeeType.CHARGE_FEE).getAmount().intValue();

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));

        TimeUnit.SECONDS.sleep(18);

        responses.add(CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference)));

        responses.forEach(response -> response.then()
                .statusCode(SC_OK)
                .body("transactionId.type", equalTo("CHARGE_FEE"))
                .body("transactionId.id", notNullValue())
                .body("profileId", equalTo(corporateProfileId))
                .body("feeType", equalTo(feesChargeModel.getFeeType()))
                .body("source.type", equalTo("managed_accounts"))
                .body("source.id", equalTo(feesChargeModel.getSource().getId()))
                .body("availableBalanceAdjustment.currency", equalTo(corporateCurrency))
                .body("availableBalanceAdjustment.amount", equalTo(Math.negateExact(chargeFee)))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue()));

        assertNotEquals(responses.get(0).jsonPath().getString("transactionId.id"), responses.get(1).jsonPath().getString("transactionId.id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));

        assertEquals(2, ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken).jsonPath().getList("entry.transactionId.type").stream().filter(x -> x.equals("CHARGE_FEE")).count());
    }

    @Test
    public void ChargeCorporateFee_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {

        final String managedAccountId = createManagedAccount();
        simulateManagedAccountDeposit(managedAccountId);

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", RandomStringUtils.randomNumeric(18)));

        final FeesChargeModel feesChargeModel1 =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        CorporatesService.chargeCorporateFee(feesChargeModel, secretKey, authenticationToken, Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_CONFLICT);

        CorporatesService.chargeCorporateFee(feesChargeModel1, secretKey, authenticationToken, Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        assertEquals(1, ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken).jsonPath().getList("entry.transactionId.type").stream().filter(x -> x.equals("CHARGE_FEE")).count());
    }

    private long getManagedAccountFees() {
        final Map<FeeType, CurrencyAmount> fees =
                TestHelper.getFees(corporateCurrency).entrySet().stream()
                        .filter(x -> x.getKey().equals(FeeType.DEPOSIT_FEE) || x.getKey().equals(FeeType.CHARGE_FEE))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fees.values().stream().mapToLong(CurrencyAmount::getAmount).sum();
    }

    private long getManagedCardFees() {
        final Map<FeeType, CurrencyAmount> fees =
                TestHelper.getFees(corporateCurrency).entrySet().stream()
                        .filter(x -> x.getKey().equals(FeeType.CHARGE_FEE))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return fees.values().stream().mapToLong(CurrencyAmount::getAmount).sum();
    }

    private void assertManagedAccountBalance(final String managedAccountId, final int expectedBalance) {
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(expectedBalance))
                .body("balances.actualBalance", equalTo(expectedBalance));
    }

    private void assertManagedCardBalance(final String managedCardId, final int expectedBalance) {
        ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(expectedBalance))
                .body("balances.actualBalance", equalTo(expectedBalance));
    }

    private void simulateManagedAccountDeposit(final String managedAccountId) {
        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency,
                MANAGED_ACCOUNT_DEPOSIT_AMOUNT, secretKey, authenticationToken);
    }

    private void simulateManagedCardDeposit(final String managedCardId) {
        TestHelper.simulateManagedAccountDepositAndTransferToCard(managedAccountProfileId, transfersProfileId,
                managedCardId, corporateCurrency, MANAGED_CARD_DEPOSIT_AMOUNT, secretKey, authenticationToken);
    }

    private String createManagedAccount() {
        return ManagedAccountsHelper.createManagedAccount(managedAccountProfileId,
                corporateCurrency,
                secretKey,
                authenticationToken);
    }

    private String createPrepaidCard() {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(prepaidCardProfileId, corporateCurrency).build();

        return ManagedCardsHelper
                .createManagedCard(createManagedCardModel, secretKey, authenticationToken);
    }

    private String createDebitCard(final String managedAccountId) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(debitCardProfileId, managedAccountId).build();

        return ManagedCardsHelper
                .createManagedCard(createManagedCardModel, secretKey, authenticationToken);
    }

    private void setCustomFee(){
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail,innovatorPassword);
        final UpdateCorporateProfileModel updateCorporateProfileModel = UpdateCorporateProfileModel.builder()
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("CUSTOM_FEE")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L)))))))).build();

        InnovatorHelper.updateCorporateProfile(updateCorporateProfileModel, innovatorToken, programmeId,corporateProfileId);
    }
}