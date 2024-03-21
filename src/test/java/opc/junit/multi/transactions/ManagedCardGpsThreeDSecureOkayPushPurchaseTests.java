package opc.junit.multi.transactions;

import commons.enums.State;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class ManagedCardGpsThreeDSecureOkayPushPurchaseTests extends BaseGpsThreeDSecureSetup {

    /**
     * 3DS Biometric Purchase
     * Tests do NOT focus on specific identities or transaction outcomes (auth and settlement), but
     * rather on integrated 3DS feature with okayThis biometrics, which happens before
     * the initialisation of the authorisation process
     */

    private static String threeDSIdentityToken;
    private static String threeDSIdentityCurrency;
    private static String identityToken;
    private static String identityCurrency;

    @BeforeAll
    public static void setup() {
        initialSetup();
        identitySetup();
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_DebitOkayAccept_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final String accountId = createManagedAccountId(threeDSIdentityToken);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKey, threeDSIdentityToken);

        TestHelper.simulateManagedAccountDeposit(accountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKey, threeDSIdentityToken );

        final String managedCardId = createDebitThreeDSecureManagedCard(user.getLeft(), accountId);

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                threeDSIdentityToken);

        SimulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), user.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, threeDSIdentityToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_PrepaidOkayAccept_Success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final String managedCardId = createPrepaidThreeDSecureManagedCard(user.getLeft());

        transferFundsToCard(threeDSIdentityToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        SimulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), user.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, threeDSIdentityToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_MultipleCardsPrepaidOkayAccept_Success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();
        final int numberOfCards = 3;

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final List<String> cards = new LinkedList<>();
        IntStream.range(0, numberOfCards)
                .forEach(i -> {
                    final String managedCardId = createPrepaidThreeDSecureManagedCard(user.getLeft());
                    transferFundsToCard(threeDSIdentityToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

                    SimulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), user.getLeft());
                    cards.add(managedCardId);
                });

        //expect authorisation and settlement to occur since biometric challenge was accepted
        for (String card : cards) {
            ManagedCardsHelper.getManagedCardStatement(card, secretKey, threeDSIdentityToken, 3)
                    .then()
                    .statusCode(SC_OK)
                    .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                    .body("entry[0].entryState", equalTo("COMPLETED"))
                    .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                    .body("entry[1].entryState", equalTo("COMPLETED"))
                    .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                    .body("entry[2].entryState", equalTo("COMPLETED"))
                    .body("count", equalTo(3))
                    .body("responseCount", equalTo(3));
        }
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_DebitOkayReject_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final String accountId = createManagedAccountId(threeDSIdentityToken);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKey, threeDSIdentityToken);

        TestHelper.simulateManagedAccountDeposit(accountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKey, threeDSIdentityToken);

        final String managedCardId = createDebitThreeDSecureManagedCard(user.getLeft(), accountId);

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                threeDSIdentityToken);

        SimulatePurchaseAndRejectOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //expect authorisation and settlement not to occur since biometric challenge was rejected
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, threeDSIdentityToken, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_PrepaidOkayReject_Success() {

        final Long depositAmount = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final String managedCardId = createPrepaidThreeDSecureManagedCard(user.getLeft());

        transferFundsToCard(threeDSIdentityToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        SimulatePurchaseAndRejectOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //expect authorisation and settlement not to occur since biometric challenge was rejected
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, threeDSIdentityToken, 1)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_InitiateBiometricNotEnabled_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final String accountId = createManagedAccountId(threeDSIdentityToken);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKey, threeDSIdentityToken);

        TestHelper.simulateManagedAccountDeposit(accountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKey, threeDSIdentityToken );

        final String managedCardId = createDebitThreeDSecureManagedCard(user.getLeft(), accountId);

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                threeDSIdentityToken);

        SimulateThreeDSecurePurchaseById(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), false);

        //expect authorisation and settlement to occur since biometric challenge was never issued
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, threeDSIdentityToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_InitiateBiometricNotProvided_Success() {

        final Long availableToSpend = 1000L;
        final Long purchaseAmount = getRandomPurchaseAmount();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .build();
        final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
                threeDSIdentityToken);

        enrollDeviceForBiometric(user);

        final String accountId = createManagedAccountId(threeDSIdentityToken);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKey, threeDSIdentityToken);

        TestHelper.simulateManagedAccountDeposit(accountId,threeDSIdentityCurrency, getRandomDepositAmount(),secretKey, threeDSIdentityToken);

        final String managedCardId = createDebitThreeDSecureManagedCard(user.getLeft(), accountId);

        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                threeDSIdentityToken);

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount))
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKey, managedCardId, simulateCardPurchaseModel);

        //expect authorisation and settlement to occur since biometric challenge was never issued
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKey, threeDSIdentityToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ThreeDSecureCardOkayPushPurchase_CardNotEnrolledForBiometricThreeDS_Success() {

        final Long purchaseAmount = getRandomPurchaseAmount();

        final String accountId = createManagedAccountId(identityToken);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKey, identityToken);

        TestHelper.simulateManagedAccountDeposit(accountId, identityCurrency, getRandomDepositAmount(), secretKey, identityToken);

        final String managedCardId = createDebitManagedCard(accountId, identityCurrency);

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(identityCurrency, purchaseAmount))
                        .setInitiateBiometricThreeDSecure(true)
                        .build();

        SimulatorService.simulateCardPurchaseById(simulateCardPurchaseModel, managedCardId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("code", equalTo("CARD_NOT_ENROLLED_FOR_BIOMETRIC_THREEDS"));
    }

    private static void identitySetup() {
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(threeDSCorporateProfileId).build();

        final Pair<String, String> threeDSAuthenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                threeDSCorporateDetails, secretKey);
        threeDSIdentityToken = threeDSAuthenticatedCorporate.getRight();
        threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                corporateDetails, secretKey);
        identityToken = authenticatedCorporate.getRight();
        identityCurrency = corporateDetails.getBaseCurrency();
    }

    private static void initialSetup() {

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.setProgrammeAuthyChallengeLimit(threeDSApp.getProgrammeId(), resetCount,
                adminToken);
    }

    private void enrollDeviceForBiometric(final Pair<String, String> user) {
        final String linkingCode = SecureHelper.enrolBiometricUser(user.getRight(),
                sharedKey);
        SimulatorHelper.acceptOkayIdentity(secretKey, user.getLeft(), linkingCode,
                user.getRight(), State.ACTIVE);
    }

    private String createManagedAccountId(final String authenticationToken) {
        return ManagedAccountsHelper.createManagedAccount(corporateManagedAccountsProfileId,
                threeDSIdentityCurrency, secretKey, authenticationToken);
    }

    private String createDebitThreeDSecureManagedCard(final String userToken,
                                                      final String managedAccountId) {
        return ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                                corporateDebitManagedCardsProfileId, userToken, managedAccountId).build(), secretKey,
                        threeDSIdentityToken, Optional.empty())
                .then()
                .extract().jsonPath().getString("id");
    }

    private String createDebitManagedCard(final String managedAccountId,
                                          final String currency) {
        return ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.DefaultCreateDebitManagedCardModel(
                                corporateDebitManagedCardsProfileId, managedAccountId, currency).build(), secretKey,
                        identityToken, Optional.empty())
                .then()
                .extract()
                .jsonPath()
                .getString("id");
    }

    private String createPrepaidThreeDSecureManagedCard(final String userId) {
        return ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                                corporatePrepaidManagedCardsProfileId, userId).build(), secretKey,
                        threeDSIdentityToken, Optional.empty())
                .then()
                .extract()
                .jsonPath()
                .getString("id");
    }
}
