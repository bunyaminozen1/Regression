package opc.junit.multi.transactions;

import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class ManagedCardGpsThreeDSecureAuthyPushMerchantRefundTests extends BaseGpsThreeDSecureSetup {

    /**
     * 3DS Authy Merchant Refund
     * Tests do NOT focus on specific identities or transaction outcomes (auth and settlement), but
     * rather on integrated 3DS feature with authy push authentication factor for merchant refund,
     * which happens before the initialisation of the authorisation process
     */

    private static String token;
    private static String currency;

    @BeforeAll
    public static void setup() {
        initialSetup();
        identitySetup();
    }

    @Test
    public void ThreeDSecureCardAuthyPushMerchantRefund_DebitAuthyAccept_Success() {

        final Pair<String, String> user = createEnrolledUserAndVerifyAuthyPush();

        final String managedCardId = createDebitAuthyManagedCardAndSetSpendLimit(createManagedAccountAndDeposit(), user.getLeft());

        SimulateMerchantRefundAndAcceptAuthyChallenge(managedCardId, new CurrencyAmount(currency, getRandomPurchaseAmount()), user.getLeft());

        //expect authorisation, settlement and merchant refund to occur since authy challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, token,3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("MERCHANT_REFUND"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardAuthyPushMerchantRefund_PrepaidAuthyAccept_Success() {

        final Pair<String, String> user = createEnrolledUserAndVerifyAuthyPush();

        final String managedCardId = createPrepaidAuthyManagedCardAndTransferFunds(user.getLeft());

        SimulateMerchantRefundAndAcceptAuthyChallenge(managedCardId, new CurrencyAmount(currency, getRandomPurchaseAmount()), user.getLeft());

        //expect authorisation, settlement and merchant refund to occur since authy challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, token,4)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("MERCHANT_REFUND"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("entry[3].transactionId.type", equalTo("TRANSFER"))
                .body("entry[3].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(4))
                .body("responseCount", equalTo(4));
    }

    @Test
    public void ThreeDSecureCardAuthyPushMerchantRefund_DebitAuthyReject_Success() {

        final Pair<String, String> user = createEnrolledUserAndVerifyAuthyPush();

        final String managedCardId = createDebitAuthyManagedCardAndSetSpendLimit(createManagedAccountAndDeposit(), user.getLeft());

        SimulateMerchantRefundAndRejectAuthyChallenge(managedCardId, new CurrencyAmount(currency, getRandomPurchaseAmount()));

        //expect authorisation, settlement and merchant refund not to occur since authy challenge was rejected
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, token, 0)
                .then()
                .statusCode(200)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void ThreeDSecureCardAuthyPushMerchantRefund_PrepaidAuthyReject_Success() {

        final Pair<String, String> user = createEnrolledUserAndVerifyAuthyPush();

        final String managedCardId = createPrepaidAuthyManagedCardAndTransferFunds(user.getLeft());

        SimulateMerchantRefundAndRejectAuthyChallenge(managedCardId, new CurrencyAmount(currency, getRandomPurchaseAmount()));

        //expect authorisation, settlement and merchant refund not to occur since authy challenge was rejected
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, token, 1)
                .then()
                .statusCode(200)
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void ThreeDSecureCardAuthyPushMerchantRefund_InitiateBiometricNotEnabled_Success() {

        final Pair<String, String> user = createEnrolledUserAndVerifyAuthyPush();

        final String managedCardId = createDebitAuthyManagedCardAndSetSpendLimit(createManagedAccountAndDeposit(), user.getLeft());

        SimulateThreeDSecureMerchantRefundById(managedCardId,
                new CurrencyAmount(currency, getRandomPurchaseAmount()), false);

        //expect authorisation, settlement and merchant refund to occur since authy challenge was never issued
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, token, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("MERCHANT_REFUND"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardAuthyPushMerchantRefund_InitiateBiometricNotProvided_Success() {

        final Pair<String, String> user = createEnrolledUserAndVerifyAuthyPush();

        final String managedCardId = createDebitAuthyManagedCardAndSetSpendLimit(createManagedAccountAndDeposit(), user.getLeft());

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundByIdModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(currency, getRandomPurchaseAmount()))
                        .build();

        SimulatorHelper.simulateMerchantRefundById(secretKey, managedCardId, simulateCardMerchantRefundByIdModel);

        //expect authorisation, settlement and merchant refund to occur since authy challenge was never issued
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, token, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("MERCHANT_REFUND"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardAuthyPushPurchase_CardNotEnrolledForThreeDS_Success() {

        final String managedCardId = createDebitDefaultManagedCard(createManagedAccountAndDeposit());

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundByIdModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(currency, getRandomPurchaseAmount()))
                        .setInitiateBiometricThreeDSecure(true)
                        .build();

        //Unable to initiate 3ds merchant refund since card is not 3ds authy enabled
        SimulatorService.simulateMerchantRefundById(simulateCardMerchantRefundByIdModel, managedCardId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("code", equalTo("CARD_NOT_ENROLLED_FOR_BIOMETRIC_THREEDS"));
    }

    //Identity and initial setup methods

    private static void identitySetup() {
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                corporateDetails, secretKey);

        token = authenticatedCorporate.getRight();
        currency = corporateDetails.getBaseCurrency();
    }

    private static void initialSetup() {

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeId, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeId, resetCount,
                adminToken);
    }

    //Methods for enrolling user, creating authy debit and prepaid cards

    private Pair<String, String> createEnrolledUserAndVerifyAuthyPush() {
        final Pair<String, String> user = UsersHelper.createEnrolledUser(UsersModel.DefaultUsersModel().build(),
                secretKey, token);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey,
                user.getRight());

        return user;
    }

    private String createManagedAccountAndDeposit() {
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountsProfileId,
                currency, secretKey, token);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, token);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, getRandomDepositAmount(), secretKey, token);

        return managedAccountId;
    }

    private String createDebitAuthyManagedCardAndSetSpendLimit(final String managedAccountId,
                                                               final String userLinkedId) {
        final String managedCardId = ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreateDebitManagedCardModel(
                                corporateDebitManagedCardsProfileId, userLinkedId, managedAccountId).build(), secretKey,
                        token, Optional.empty())
                .then()
                .extract().jsonPath().getString("id");

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(currency, 1000L), LimitInterval.ALWAYS)),
                token);

        return managedCardId;
    }

    private String createDebitDefaultManagedCard(final String managedAccountId) {
        final String managedCardId =  ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.DefaultCreateDebitManagedCardModel(
                                corporateDebitManagedCardsProfileId, managedAccountId, currency).build(), secretKey,
                        token, Optional.empty())
                .then()
                .extract().jsonPath().getString("id");

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(currency, 1000L), LimitInterval.ALWAYS)),
                token);

        return managedCardId;
    }

    private String createPrepaidAuthyManagedCardAndTransferFunds(final String userLinkedId) {
        final String managedCardId = ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.AuthyThreeDSecureCreatePrepaidManagedCardModel(
                                corporatePrepaidManagedCardsProfileId, userLinkedId).build(), secretKey,
                        token, Optional.empty())
                .then()
                .extract().jsonPath().getString("id");

        transferFundsToCard(token, managedCardId, currency, 1000L, 1);

        return managedCardId;
    }
}