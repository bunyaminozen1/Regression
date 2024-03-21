package opc.junit.multi.transactions;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.GPS_THREED_SECURE_TRANSACTIONS)
public class BaseGpsThreeDSecureSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel threeDSApp;
    protected static String corporateProfileId;
    protected static String threeDSCorporateProfileId;

    protected static String secretKey;
    protected static String sharedKey;
    protected static String programmeId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String transfersProfileId;

    protected static String innovatorToken;

    @BeforeAll
    public static void GlobalSetup() {
        threeDSApp = (ProgrammeDetailsModel) setupExtension.store.get(
                InnovatorSetup.THREE_DS_APP);

        corporateProfileId = threeDSApp.getCorporatesProfileId();
        threeDSCorporateProfileId = threeDSApp.getThreeDSCorporatesProfileId();
        secretKey = threeDSApp.getSecretKey();
        sharedKey = threeDSApp.getSharedKey();
        programmeId = threeDSApp.getProgrammeId();

        corporatePrepaidManagedCardsProfileId = threeDSApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = threeDSApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountsProfileId = threeDSApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        transfersProfileId = threeDSApp.getTransfersProfileId();

        innovatorToken = InnovatorHelper.loginInnovator(threeDSApp.getInnovatorEmail(),
                threeDSApp.getInnovatorPassword());
    }

    protected void SimulatePurchaseAndAcceptOkayChallenge(final String managedCardId,
                                                          final CurrencyAmount transactionAmount,
                                                          final String credentialId) {
        final String challengeId = SimulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.acceptOkayThreeDSecureChallenge(secretKey, challengeId, credentialId);
    }

    protected void SimulatePurchaseAndRejectOkayChallenge(final String managedCardId,
                                                        final CurrencyAmount transactionAmount) {
        final String challengeId = SimulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.rejectOkayThreeDSecureChallenge(secretKey, challengeId);
    }

    protected void SimulatePurchaseAndAcceptAuthyChallenge(final String managedCardId,
                                                           final CurrencyAmount transactionAmount,
                                                           final String credentialId) {
        final String challengeId = SimulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.acceptAuthyThreeDSecureChallenge(secretKey, challengeId, credentialId);
    }

    protected void SimulatePurchaseAndRejectAuthyChallenge(final String managedCardId,
                                                          final CurrencyAmount transactionAmount) {
        final String challengeId = SimulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.rejectAuthyThreeDSecureChallenge(secretKey, challengeId);
    }

    protected String SimulateThreeDSecurePurchaseById(final String managedCardId,
                                                      final CurrencyAmount transactionAmount,
                                                      final Boolean initiateBiometric) {
        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(transactionAmount)
                        .setInitiateBiometricThreeDSecure(initiateBiometric)
                        .build();

        if (initiateBiometric)
            return SimulatorHelper.simulateThreeDSecureCardPurchaseById(secretKey, managedCardId, simulateCardPurchaseModel);
        else {
            SimulatorHelper.simulateCardPurchaseById(secretKey, managedCardId, simulateCardPurchaseModel);
            return null;
        }
    }

    protected void SimulateMerchantRefundAndAcceptOkayChallenge(final String managedCardId,
                                                                final CurrencyAmount transactionAmount,
                                                                final String credentialId) {
        final String challengeId = SimulateThreeDSecureMerchantRefundById(managedCardId, transactionAmount, true);
        SimulatorHelper.acceptOkayThreeDSecureChallenge(secretKey, challengeId, credentialId);
    }

    protected void SimulateMerchantRefundAndRejectOkayChallenge(final String managedCardId,
                                                                final CurrencyAmount transactionAmount) {
        final String challengeId = SimulateThreeDSecureMerchantRefundById(managedCardId, transactionAmount, true);
        SimulatorHelper.rejectOkayThreeDSecureChallenge(secretKey, challengeId);
    }

    protected void SimulateMerchantRefundAndAcceptAuthyChallenge(final String managedCardId,
                                                                 final CurrencyAmount transactionAmount,
                                                                 final String credentialId) {
        final String challengeId = SimulateThreeDSecureMerchantRefundById(managedCardId, transactionAmount, true);
        SimulatorHelper.acceptAuthyThreeDSecureChallenge(secretKey, challengeId, credentialId);
    }

    protected void SimulateMerchantRefundAndRejectAuthyChallenge(final String managedCardId,
                                                                final CurrencyAmount transactionAmount) {
        final String challengeId = SimulateThreeDSecureMerchantRefundById(managedCardId, transactionAmount, true);
        SimulatorHelper.rejectAuthyThreeDSecureChallenge(secretKey, challengeId);
    }

    protected String SimulateThreeDSecureMerchantRefundById(final String managedCardId,
                                                            final CurrencyAmount transactionAmount,
                                                            final Boolean initiateBiometric) {
        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundByIdModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(transactionAmount)
                        .setInitiateBiometricThreeDSecure(initiateBiometric)
                        .build();

        if (initiateBiometric)
            return SimulatorHelper.simulateThreeDSecureMerchantRefundById(secretKey, managedCardId, simulateCardMerchantRefundByIdModel);
        else {
            SimulatorHelper.simulateMerchantRefundById(secretKey, managedCardId, simulateCardMerchantRefundByIdModel);
            return null;
        }
    }

    protected void setSpendLimit(final String managedCardId,
                               final List<SpendLimitModel> spendLimit,
                               final String authenticationToken){
        final SpendRulesModel spendRulesModel = SpendRulesModel
                .builder()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                .setAllowedMerchantIds(new ArrayList<>())
                .setBlockedMerchantIds(new ArrayList<>())
                .setAllowContactless(true)
                .setAllowAtm(true)
                .setAllowECommerce(true)
                .setAllowCashback(true)
                .setAllowCreditAuthorisations(true)
                .setSpendLimit(spendLimit)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    protected static Pair<String, CreateManagedAccountModel> transferFundsToCard(final String token,
                                                                                 final String managedCardId,
                                                                                 final String currency,
                                                                                 final Long depositAmount,
                                                                                 final int transferCount){

        return TestHelper
                .simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountsProfileId,
                        transfersProfileId, managedCardId, currency, depositAmount, secretKey, token, transferCount);
    }

    protected static Long getRandomPurchaseAmount() {
        Random random = new Random();
        return (long) (random.nextInt(10 - 5) + 5) * 10;
    }

    protected static Long getRandomDepositAmount() {
        Random random = new Random();
        return (long) (random.nextInt(50 - 20) + 20) * 10;
    }
}