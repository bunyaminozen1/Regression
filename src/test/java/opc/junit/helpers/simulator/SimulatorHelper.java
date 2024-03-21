package opc.junit.helpers.simulator;

import commons.enums.State;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.database.AuthyDatabaseHelper;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.database.OkayDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.secure.BiometricPinModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.DetokenizeModel;
import opc.models.simulator.SimulateCardAuthModel;
import opc.models.simulator.SimulateCardAuthReversalByIdModel;
import opc.models.simulator.SimulateCardAuthReversalModel;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.models.simulator.SimulateCardSettlementModel;
import opc.models.simulator.SimulateCreateCollectionModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.models.simulator.SimulateDepositModel;
import opc.models.simulator.SimulateOctMerchantRefundModel;
import opc.models.simulator.SimulateOctModel;
import opc.models.simulator.SimulatePasscodeModel;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.simulator.SimulatorService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class SimulatorHelper {

    public static void simulateSecretExpiry(final String programmeKey, final String credentialsId){
        TestHelper.ensureAsExpected(15, () -> SimulatorService.simulateSecretExpiry(programmeKey, credentialsId), SC_OK);
    }

    public static void simulateCancelledAuthorisation(final String tenantId,
                                                      final SimulateCardAuthModel simulateCardAuthModel,
                                                      final String cardId) {

        final String authorisationId = TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateCardAuthorisation(simulateCardAuthModel, tenantId, cardId),
                SC_OK)
                .jsonPath().get("authorisationId");

        checkAuthorisationState(60, authorisationId, "CLOSED");
    }

    public static void simulateAuthorisation(final String tenantId,
                                             final SimulateCardAuthModel simulateCardAuthModel,
                                             final String cardId) {

        final String authorisationId = TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateCardAuthorisation(simulateCardAuthModel, tenantId, cardId),
                SC_OK)
                .jsonPath().get("authorisationId");

        checkAuthorisationState(30, authorisationId, "PENDING_SETTLEMENT");
    }

    public static String simulateAuthorisationWithState(final String tenantId,
                                             final SimulateCardAuthModel simulateCardAuthModel,
                                             final String cardId,
                                             final String expectedState) {

        final String authorisationId = TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.simulateCardAuthorisation(simulateCardAuthModel, tenantId, cardId),
                        SC_OK)
                .jsonPath().get("authorisationId");

        checkAuthorisationState(30, authorisationId, expectedState);
        return authorisationId;
    }

    public static String simulateAuthorisation(final String tenantId,
                                               final CurrencyAmount purchaseAmount,
                                               final String relatedAuthorisationId,
                                               final String cardId) {
        final SimulateCardAuthModel.Builder simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(purchaseAmount);

        if (relatedAuthorisationId != null){
            simulateCardAuthModel.setRelatedAuthorisationId(Long.parseLong(relatedAuthorisationId));
        }

        final String authorisationId = TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateCardAuthorisation(simulateCardAuthModel.build(), tenantId, cardId),
                SC_OK)
                .jsonPath().get("authorisationId");

        checkAuthorisationState(30, authorisationId, "PENDING_SETTLEMENT");

        return authorisationId;
    }

    public static String simulateAuthorisation(final String tenantId,
                                               final CurrencyAmount purchaseAmount,
                                               final String relatedAuthorisationId,
                                               final String cardId,
                                               final Boolean expectPendingSettlement) {
        final SimulateCardAuthModel.Builder simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(purchaseAmount);

        if (relatedAuthorisationId != null){
            simulateCardAuthModel.setRelatedAuthorisationId(Long.parseLong(relatedAuthorisationId));
        }

        final String authorisationId = TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.simulateCardAuthorisation(simulateCardAuthModel.build(), tenantId, cardId),
                        SC_OK)
                .jsonPath().get("authorisationId");

        if (expectPendingSettlement) {
            checkAuthorisationState(30, authorisationId, "PENDING_SETTLEMENT");
        } else {
            checkAuthorisationState(30, authorisationId, "CLOSED");
        }

        return authorisationId;
    }

    public static String simulateAuthorisationWithTimestamp(final String tenantId,
                                                            final CurrencyAmount purchaseAmount,
                                                            final String cardId,
                                                            final Long timestamp,
                                                            final boolean isSuccessfulCall) {
        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(purchaseAmount)
                        .setAuthTransactionTimestamp(timestamp).build();

        final String response = TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateCardAuthorisation(simulateCardAuthModel, tenantId, cardId),
                isSuccessfulCall ? SC_OK : SC_CONFLICT)
                .jsonPath().get(isSuccessfulCall ? "authorisationId":"code");

        if (isSuccessfulCall){
            checkAuthorisationState(30, response, "PENDING_SETTLEMENT");
        }

        return response;
    }

    public static String simulateSettlement(final String tenantId,
                                            final Long relatedAuthorisationId,
                                            final CurrencyAmount purchaseAmount,
                                            final String cardId){
        final SimulateCardSettlementModel simulateCardSettlementModel =
                SimulateCardSettlementModel.DefaultSimulateCardSettlement(purchaseAmount, relatedAuthorisationId)
                        .build();

        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateCardSettlement(simulateCardSettlementModel, tenantId, cardId),
                SC_OK)
                .jsonPath().get("settlementId");
    }

    public static String simulateCardPurchaseById(final String secretKey,
                                                  final String cardId,
                                                  final CurrencyAmount transactionAmount){
        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(transactionAmount)
                        .build();

        return simulateCardPurchaseById(secretKey, cardId, simulateCardPurchaseModel);
    }

    public static String simulateAtmWithdrawalById(final String secretKey,
                                                  final String cardId,
                                                  final CurrencyAmount transactionAmount){
        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(transactionAmount)
                        .setAtmWithdrawal(true)
                        .build();

        return simulateCardPurchaseById(secretKey, cardId, simulateCardPurchaseModel);
    }

    public static String simulateCardPurchaseById(final String secretKey,
                                                  final String cardId,
                                                  final SimulateCardPurchaseByIdModel simulateCardPurchaseModel){

        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateCardPurchaseById(simulateCardPurchaseModel, cardId, secretKey),
                SC_OK)
                .jsonPath().get("code");
    }

    public static String simulateAuthReversal(final String secretKey,
                                              final String cardNumber,
                                              final String cvv,
                                              final String expiryDate,
                                              final CurrencyAmount transactionAmount){
        final SimulateCardAuthReversalModel simulateCardAuthReversalModel =
                SimulateCardAuthReversalModel.builder()
                        .setCardNumber(cardNumber)
                        .setCvv(cvv)
                        .setExpiryDate(expiryDate)
                        .setTransactionAmount(transactionAmount)
                        .build();

        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateAuthReversal(simulateCardAuthReversalModel, secretKey),
                SC_OK)
                .jsonPath().get("code");
    }

    public static String simulateMerchantRefundById(final String secretKey,
                                                    final String managedCardId,
                                                    final CurrencyAmount transactionAmount){
        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(transactionAmount)
                        .build();

        return simulateMerchantRefundById(secretKey, managedCardId, simulateCardMerchantRefundModel);
    }

    public static String simulateMerchantRefundById(final String secretKey,
                                                    final String managedCardId,
                                                    final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel){

        return TestHelper.ensureAsExpected(30,
                () -> SimulatorService.simulateMerchantRefundById(simulateCardMerchantRefundModel, managedCardId, secretKey),
                SC_OK)
                .jsonPath().get("code");
    }

    public static void simulateOct(final String secretKey,
                                   final String managedCardId,
                                   final String authenticationToken,
                                   final CurrencyAmount transactionAmount){

        final JsonPath cardDetails =
                ManagedCardsHelper.getManagedCard(secretKey, managedCardId, authenticationToken).jsonPath();

        final SimulateOctModel simulateOctModel =
                SimulateOctModel.builder()
                        .setCardNumber(getCardNumber(secretKey, cardDetails.getString("cardNumber.value"), authenticationToken))
                        .setCvv(getCvv(secretKey, cardDetails.getString("cvv.value"), authenticationToken))
                        .setExpiryDate(cardDetails.getString("expiryMmyy"))
                        .setTransactionAmount(transactionAmount)
                        .build();

        TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.simulateOct(simulateOctModel, secretKey),
                        SC_OK);
    }

    public static String getCardNumber(final String secretKey,
                                       final String encryptedCardNumber,
                                       final String authenticationToken){
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.detokenize(secretKey,
                        new DetokenizeModel(encryptedCardNumber, "CARD_NUMBER"),
                        authenticationToken), SC_OK)
                .jsonPath().get("value");
    }

    public static String getCvv(final String secretKey,
                                final String encryptedCvv,
                                final String authenticationToken){
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.detokenize(secretKey,
                        new DetokenizeModel(encryptedCvv, "CARD_NUMBER"),
                        authenticationToken), SC_OK)
                .jsonPath().get("value");
    }

    public static String simulateAuthReversalById(final String tenantId,
                                                  final CurrencyAmount purchaseAmount,
                                                  final String relatedAuthorisationId,
                                                  final String cardId) {

        final SimulateCardAuthReversalByIdModel simulateCardAuthReversalByIdModel =
                SimulateCardAuthReversalByIdModel.DefaultCardAuthModel(purchaseAmount)
                        .setRelatedAuthorisationId(Long.parseLong(relatedAuthorisationId))
                        .build();

        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateAuthReversalById(simulateCardAuthReversalByIdModel, cardId, tenantId),
                SC_OK)
                .jsonPath().get("authorisationReversalId");
    }

    public static void simulateOctThroughMerchantRefund(final SimulateOctMerchantRefundModel simulateOctMerchantRefundModel,
                                                        final String innovatorId,
                                                        final String managedCardId){
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateOctThroughMerchantRefund(simulateOctMerchantRefundModel, innovatorId, managedCardId),
                SC_OK);
    }

    public static void simulateDeposit(final String currency,
                                       final Long depositAmount,
                                       final String secretKey,
                                       final String managedAccountId){

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(currency, depositAmount));

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId),
                SC_OK);
    }

    public static String simulateOct(final String managedCardId,
                                     final long amount,
                                     final String authenticationToken,
                                     final String secretKey,
                                     final String currency) {

        final long timestamp = Instant.now().toEpochMilli();
        SimulatorHelper.simulateOct(secretKey, managedCardId, authenticationToken, new CurrencyAmount(currency, amount));

        return TestHelper.ensureDatabaseDataRetrieved(60,
                () -> ManagedCardsDatabaseHelper.getLatestOct(managedCardId, timestamp),
                x -> x.size() > 0).get(0).get("id");
    }

    public static void acceptAuthyIdentity(final String programmeKey,
                                               final String identityId,
                                               final String authenticationToken,
                                               final State expectedState){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptAuthyIdentity(programmeKey, identityId),
                SC_NO_CONTENT);

        checkAuthenticationFactorState(programmeKey, authenticationToken, expectedState);
    }

    public static void rejectAuthyIdentity(final String programmeKey,
                                           final String identityId,
                                           final String authenticationToken,
                                           final State expectedState){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.rejectAuthyIdentity(programmeKey, identityId),
                SC_NO_CONTENT);

        checkAuthenticationFactorState(programmeKey, authenticationToken, expectedState);
    }

    public static void acceptAuthyOwt(final String programmeKey,
                                      final String owtId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptAuthyOwt(programmeKey, owtId), SC_NO_CONTENT);
    }

    public static void rejectAuthyOwt(final String programmeKey,
                                      final String owtId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.rejectAuthyOwt(programmeKey, owtId), SC_NO_CONTENT);
    }

    public static void acceptAuthyStepUp(final String programmeKey, final String sessionId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptAuthyStepUp(programmeKey, sessionId), SC_NO_CONTENT);
    }

    public static void rejectAuthyStepUp(final String programmeKey, final String sessionId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.rejectAuthyStepUp(programmeKey, sessionId), SC_NO_CONTENT);
    }

    public static void successfullyDeclinedAuthyStepUp(final String programmeKey,
                                                       final String challengeId){

        acceptAuthyStepUp(programmeKey, challengeId);

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> AuthSessionsDatabaseHelper.getChallenge(challengeId),
                x -> x.get(0).get("status").equals("DECLINED"),
                Optional.of(String.format("Authy stepUp with challenge id %s not DECLINED as expected", challengeId)));
    }

    public static void acceptBiometricStepUp(final String programmeKey, final String sessionId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptBiometricStepUp(programmeKey, sessionId), SC_NO_CONTENT);
    }

    public static void rejectBiometricStepUp(final String programmeKey, final String sessionId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.rejectBiometricStepUp(programmeKey, sessionId), SC_NO_CONTENT);
    }

    public static Response createMandate(final SimulateCreateMandateModel createMandateModel,
                                         final String programmeKey,
                                         final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.createMandate(createMandateModel, programmeKey, authenticationToken), SC_OK);
    }

    public static Response createMandateCollection(final SimulateCreateCollectionModel createCollectionModel,
                                                   final String mandateId,
                                                   final String programmeKey,
                                                   final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.createMandateCollection(createCollectionModel, mandateId, programmeKey, authenticationToken), SC_OK);
    }

    public static Response cancelMandate(final String mandateId,
                                         final String programmeKey,
                                         final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.cancelMandate(mandateId, programmeKey, authenticationToken), SC_NO_CONTENT);
    }

    public static Response expireMandate(final String mandateId,
                                         final String programmeKey,
                                         final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> SimulatorService.expireMandate(mandateId, programmeKey, authenticationToken), SC_NO_CONTENT);
    }

    public static void simulateEnrolmentLinking(final String programmeKey,
                                                final String identityId,
                                                final String linkingCode){
        TestHelper.ensureAsExpected(30,
                () -> SimulatorService.simulateEnrolmentLinking(programmeKey, identityId, linkingCode),
                SC_NO_CONTENT);
    }

    public static void simulateEnrolmentUnlinking(final String programmeKey,
                                                  final String identityId,
                                                  final String linkingCode){
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateOkayUnlinking(programmeKey, identityId, linkingCode),
                SC_NO_CONTENT);
    }

    public static void acceptOkayIdentity(final String programmeKey,
                                          final String identityId,
                                          final String linkingCode,
                                          final String authenticationToken,
                                          final State expectedState){

                try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        simulateEnrolmentLinking(programmeKey, identityId, linkingCode);

        checkAuthenticationFactorState(programmeKey, authenticationToken, expectedState);
    }

    public static void rejectOkayIdentity(final String programmeKey,
                                          final String identityId,
                                          final String authenticationToken,
                                          final State expectedState){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.rejectOkayIdentity(programmeKey, identityId),
                SC_NO_CONTENT);

        checkAuthenticationFactorState(programmeKey, authenticationToken, expectedState);
    }

    public static void acceptOkayOwt(final String programmeKey,
                                     final String owtId){
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptOkayOwt(programmeKey, owtId),
                SC_NO_CONTENT);
    }

    public static void successfullyAcceptOkayOwt(final String programmeKey,
                                                 final String challengeId){

        acceptOkayOwt(programmeKey, challengeId);

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> AuthSessionsDatabaseHelper.getChallenge(challengeId),
                x -> x.get(0).get("status").equals("COMPLETED"),
                Optional.of(String.format("OWT with challenge id %s not COMPLETED as expected", challengeId)));
    }

    public static void rejectOkayOwt(final String programmeKey,
                                     final String owtId){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.rejectOkayOwt(programmeKey, owtId),
                SC_NO_CONTENT);
    }

    public static void successfullyRejectOkayOwt(final String programmeKey,
                                                 final String challengeId){

        rejectOkayOwt(programmeKey, challengeId);

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> AuthSessionsDatabaseHelper.getChallenge(challengeId),
                x -> x.get(0).get("status").equals("DECLINED"),
                Optional.of(String.format("OWT with challenge id %s not DECLINED as expected", challengeId)));
    }
    public static void acceptOkayThreeDSecureChallenge(final String programmeKey,
                                                       final String challengeId) {
        TestHelper.ensureAsExpected(30,
                () -> SimulatorService.acceptOkayThreeDSecureChallenge(programmeKey, challengeId),
                SC_NO_CONTENT);
    }

    @SneakyThrows
    public static void acceptOkayThreeDSecureChallenge(final String programmeKey,
                                                       final String challengeId,
                                                       final String credentialId) {
        TestHelper.ensureAsExpected(30,
            () -> SimulatorService.acceptOkayThreeDSecureChallenge(programmeKey, challengeId),
            SC_NO_CONTENT);

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> OkayDatabaseHelper.getUserRequest(credentialId),
                x -> !x.isEmpty() && new ArrayList<>(x.values()).stream().allMatch(y -> "VERIFIED".equals(y.get("status"))),
                Optional.of(String.format("Okay request status for identity with id %s not VERIFIED", credentialId)));
    }

    public static void rejectOkayThreeDSecureChallenge(final String programmeKey,
                                                       final String challengeId) {
        TestHelper.ensureAsExpected(30,
                () -> SimulatorService.rejectOkayThreeDSecureChallenge(programmeKey, challengeId),
                SC_NO_CONTENT);
    }

    public static void acceptAuthyThreeDSecureChallenge(final String programmeKey,
                                                        final String challengeId,
                                                        final String credentialId) {
        TestHelper.ensureAsExpected(30,
                () -> SimulatorService.acceptAuthyThreeDSecureChallenge(programmeKey, challengeId),
                SC_NO_CONTENT);

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> AuthyDatabaseHelper.getUserRequest(credentialId),
                x -> !x.isEmpty() && new ArrayList<>(x.values()).stream().allMatch(y -> "VERIFIED".equals(y.get("status"))),
                Optional.of(String.format("Authy request status for identity with id %s not VERIFIED", credentialId)));
    }

    public static void rejectAuthyThreeDSecureChallenge(final String programmeKey,
                                                        final String challengeId) {
        TestHelper.ensureAsExpected(30,
                () -> SimulatorService.rejectAuthyThreeDSecureChallenge(programmeKey, challengeId),
                SC_NO_CONTENT);
    }

    public static String simulateThreeDSecureCardPurchaseById(final String secretKey,
                                                              final String cardId,
                                                              final SimulateCardPurchaseByIdModel simulateCardPurchaseModel){

        return TestHelper.ensureAsExpected(30,
                        () -> SimulatorService.simulateCardPurchaseById(simulateCardPurchaseModel, cardId, secretKey),
                        SC_OK)
                .jsonPath().get("threeDSecureChallengeId.value");
    }

    public static String simulateThreeDSecureMerchantRefundById(final String secretKey,
                                                                final String cardId,
                                                                final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundByIdModel){

        return TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.simulateMerchantRefundById(simulateCardMerchantRefundByIdModel, cardId, secretKey),
                        SC_OK)
                .jsonPath().get("threeDSecureChallengeId.value");
    }

    public static void okayOwtWithPin(final String programmeKey,
                                      final String owtId,
                                      final String pin,
                                      final String expectedState){

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.okayOwtPinFallback(programmeKey, owtId, new SimulatePasscodeModel(pin)),
                SC_NO_CONTENT);

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> AuthSessionsDatabaseHelper.getChallenge(owtId),
                x -> x.get(0).get("status").equals(expectedState),
                Optional.of(String.format("OWT with id %s not in state %s as expected", owtId, expectedState)));
    }

    public static void acceptOkayLoginChallenge(final String programmeKey,
        final String challengeId){
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.acceptOkayLoginChallenge(programmeKey, challengeId),
            SC_NO_CONTENT);
    }

    public static void enterPinOkayLoginChallenge(final String programmeKey,
                                                  final String challengeId,
                                                  final BiometricPinModel passcode){
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.enterPinOkayLoginChallenge(programmeKey, challengeId, passcode),
                SC_NO_CONTENT);
    }

    public static void rejectOkayLoginChallenge(final String programmeKey,
        final String challengeId){
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.rejectOkayLoginChallenge(programmeKey, challengeId),
            SC_NO_CONTENT);
    }

    public static void rejectOkayBeneficiaryBatch(final String programmeKey,
                                                  final String batchId){
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.rejectOkayBeneficiaryBatch(programmeKey, batchId),
            SC_NO_CONTENT);
    }

    public static void acceptAuthyBeneficiaryBatch(final String programmeKey,
                                                   final String batchId){
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.acceptAuthyBeneficiaryBatch(programmeKey, batchId),
            SC_NO_CONTENT);
    }

    public static void rejectAuthyBeneficiaryBatch(final String programmeKey,
                                                   final String batchId){
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.rejectAuthyBeneficiaryBatch(programmeKey, batchId),
            SC_NO_CONTENT);
    }

    private static void checkAuthenticationFactorState(final String programmeKey,
                                                       final String authenticationToken,
                                                       final State expectedState) {

        TestHelper.ensureAsExpected(60,
                        () -> AuthenticationFactorsService.getAuthenticationFactors(programmeKey, Optional.empty(), authenticationToken),
                        x -> x.statusCode() == SC_OK && x.jsonPath().getString("factors[0].status")
                                .equals(expectedState.name()),
                Optional.of(String.format("Expecting 200 with an authentication factor in state %s, check logged payload", expectedState.name())));
    }

    private static void checkAuthorisationState(final int timeout,
                                                final String authorisationId,
                                                final String expectedState) {
        TestHelper.ensureDatabaseResultAsExpected(timeout,
                () -> ManagedCardsDatabaseHelper.getAuthorisation(authorisationId),
                x -> x.get(0).get("auth_state").equals(expectedState),
                Optional.of(String.format("Authorisation with id %s not in state %s as expected", authorisationId, expectedState)));
    }

    public static void acceptAuthyChallenge(final String programmeKey,
                                             final String scaChallengeId) {
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.acceptAuthyChallenge(programmeKey, scaChallengeId),
            SC_NO_CONTENT);
    }

    public static void rejectAuthyChallenge(final String programmeKey,
                                             final String scaChallengeId) {
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.rejectAuthyChallenge(programmeKey, scaChallengeId),
            SC_NO_CONTENT);
    }

    public static void acceptOkayChallenge(final String programmeKey,
                                            final String scaChallengeId) {
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.acceptOkayChallenge(programmeKey, scaChallengeId),
            SC_NO_CONTENT);
    }

    public static void rejectOkayChallenge(final String programmeKey,
                                            final String scaChallengeId) {
        TestHelper.ensureAsExpected(15,
            () -> SimulatorService.rejectOkayChallenge(programmeKey, scaChallengeId),
            SC_NO_CONTENT);
    }

    public static void acceptAuthySend(final String programmeKey,
                                       final String sendId) {
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptAuthySend(programmeKey, sendId),
                SC_NO_CONTENT);
    }

    public static void acceptOkaySend(final String programmeKey,
                                      final String sendId) {
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.acceptOkaySend(programmeKey, sendId),
                SC_NO_CONTENT);
    }
}