package opc.services.simulator;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.UrlType;
import opc.models.secure.BiometricPinModel;
import opc.models.simulator.DetokenizeModel;
import opc.models.simulator.SetCannedResponseModel;
import opc.models.simulator.SimulateCardAuthModel;
import opc.models.simulator.SimulateCardAuthReversalByIdModel;
import opc.models.simulator.SimulateCardAuthReversalModel;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.models.simulator.SimulateCardMerchantRefundModel;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.models.simulator.SimulateCardPurchaseModel;
import opc.models.simulator.SimulateCardSettlementModel;
import opc.models.simulator.SimulateCreateCollectionModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.models.simulator.SimulateDepositByIbanModel;
import opc.models.simulator.SimulateDepositModel;
import opc.models.simulator.SimulateOctMerchantRefundModel;
import opc.models.simulator.SimulateOctModel;
import opc.models.simulator.SimulatePasscodeModel;
import opc.models.simulator.SimulatePendingDepositModel;
import opc.models.simulator.SimulateSecretExpiryModel;
import opc.models.simulator.TokenizeModel;
import commons.services.BaseService;

public class SimulatorService extends BaseService {

    public static Response simulateKybApproval(final String programmeKey,
                                               final String corporateId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("corporate_id", corporateId)
                .when()
                .post("/simulate/api/corporates/{corporate_id}/verify");
    }

    public static Response simulateKycApproval(final String programmeKey,
                                               final String consumerId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("consumer_id", consumerId)
                .when()
                .post("/simulate/api/consumers/{consumer_id}/verify");
    }

    public static  Response simulateManagedAccountDeposit(final SimulateDepositModel simulateDepositModel,
                                                         final String programmeKey,
                                                         final String managedAccountId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("account_id", managedAccountId)
                .and()
                .body(simulateDepositModel)
                .when()
                .post("/simulate/api/accounts/{account_id}/deposit");
    }

    public static Response simulateDepositByIban(final SimulateDepositByIbanModel simulateDepositByIbanModel,
        final String programmeKey) {
        return simulatorRestAssured()
            .header("Content-type", "application/json")
            .header("programme-key", programmeKey)
            .and()
            .body(simulateDepositByIbanModel)
            .when()
            .post("/simulate/api/accounts/deposit");
    }

    public static Response simulateManagedAccountPendingDeposit(final SimulatePendingDepositModel simulatePendingDepositModel,
                                                                final String tenantId,
                                                                final String managedAccountId) {
        return oldSimulatorRestAssured()
            .header("Content-type", "application/json")
            .header("tenant-id", tenantId)
            .pathParam("account_id", managedAccountId)
            .and()
            .body(simulatePendingDepositModel)
            .when()
            .post("/api/account_processing_simulator/paynetics_simulator/account_simulator/{account_id}/pending_deposit");
    }

    public static Response simulateManagedAccountPendingDepositByIban(final SimulatePendingDepositModel simulatePendingDepositModel,
        final String tenantId,
        final String IbanId) {
        return oldSimulatorRestAssured()
            .header("Content-type", "application/json")
            .header("tenant-id", tenantId)
            .pathParam("iban_id", IbanId)
            .and()
            .body(simulatePendingDepositModel)
            .when()
            .post("/api/iban_processing_simulator/paynetics_simulator/iban_simulator/{iban_id}/pending_deposit");
    }

    public static Response simulateSecretExpiry(final String programmeKey, final String credentialId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(new SimulateSecretExpiryModel(credentialId))
                .when()
                .post("/simulate/api/secrets/expire_secret");
    }

    public static Response tokenize(final String programmeKey,
                                    final TokenizeModel tokenizeModel,
                                    final String token) {
        return simulatorRestAssured()
                .header("Authorization", token)
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(tokenizeModel)
                .when()
                .post("/simulate/api/secure_session/tokenize");
    }

    public static Response tokenizeAnon(final String programmeKey,
                                        final TokenizeModel tokenizeModel) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(tokenizeModel)
                .when()
                .post("/simulate/api/secure_session/anon_tokenize");
    }

    public static Response detokenize(final String programmeKey,
                                      final DetokenizeModel detokenizeModel,
                                      final String token) {
        return simulatorRestAssured()
                .header("Authorization", token)
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(detokenizeModel)
                .when()
                .post("/simulate/api/secure_session/detokenize");
    }

    public static Response simulateCardPurchase(final SimulateCardPurchaseModel simulateCardPurchaseModel,
                                                final String programmeKey) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(simulateCardPurchaseModel)
                .when()
                .post("/simulate/api/cards/purchase");
    }

    public static Response simulateCardPurchaseById(final SimulateCardPurchaseByIdModel simulateCardPurchaseModel,
                                                    final String cardId,
                                                    final String programmeKey) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("card_id", cardId)
                .and().body(simulateCardPurchaseModel)
                .when()
                .post("/simulate/api/cards/{card_id}/purchase");
    }

    public static Response simulateCardAuthorisation(final SimulateCardAuthModel simulateCardAuthModel,
                                                     final String tenantId,
                                                     final String cardId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("card_id", cardId)
                .and()
                .body(simulateCardAuthModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/{card_id}/authorisation");
    }

    public static Response simulateCardSettlement(final SimulateCardSettlementModel simulateCardSettlementModel,
                                                  final String tenantId,
                                                  final String cardId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("card_id", cardId)
                .and()
                .body(simulateCardSettlementModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/{card_id}/settlement");
    }

    public static Response simulateAuthReversal(final SimulateCardAuthReversalModel simulateCardAuthReversalModel,
                                                final String programmeKey) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(simulateCardAuthReversalModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/auth_reversal");
    }

    public static Response simulateAuthReversalById(final SimulateCardAuthReversalByIdModel simulateCardAuthReversalModel,
                                                    final String cardId,
                                                    final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("card_id", cardId)
                .and().body(simulateCardAuthReversalModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/{card_id}/authorisation_reversal");
    }

    public static Response simulateMerchantRefund(final SimulateCardMerchantRefundModel simulateCardMerchantRefundModel,
                                                  final String programmeKey) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(simulateCardMerchantRefundModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/merchant_refund");
    }

    public static Response simulateMerchantRefundById(final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel,
                                                      final String cardId,
                                                      final String programmeKey) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("card_id", cardId)
                .and().body(simulateCardMerchantRefundModel)
                .when()
                .post("/simulate/api/cards/{card_id}/merchant_refund");
    }

    public static Response simulateOct(final SimulateOctModel simulateOctModel,
                                       final String programmeKey) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .and().body(simulateOctModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/oct");
    }

    public static Response simulateOvernightSweep(final String tenantId, final String managedCardId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("managedCardId", managedCardId)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/{managedCardId}/overnight_sweep");
    }

    public static Response simulateOctThroughMerchantRefund(final SimulateOctMerchantRefundModel simulateOctMerchantRefundModel,
                                                            final String tenantId,
                                                            final String managedCardId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("managedCardId", managedCardId)
                .and()
                .body(simulateOctMerchantRefundModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/card_simulator/{managedCardId}/oct_refund");
    }

    public static Response setManagedCardCannedResponse(final SetCannedResponseModel setCannedResponseModel,
                                                        final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .and()
                .body(setCannedResponseModel)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/canned_responses/put");
    }

    public static Response clearManagedCardCannedResponse(final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/canned_responses/clear");
    }

    public static Response getManagedCardCannedResponse(final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .when()
                .post("/api/card_processing_simulator/gps_simulator/canned_responses/peek");
    }

    protected static RequestSpecification simulatorRestAssured() {
        return restAssured(UrlType.SIMULATOR);
    }

    protected static RequestSpecification oldSimulatorRestAssured() {
        return restAssured(UrlType.OLD_SIMULATOR);
    }


    public static Response getChallengeVerificationCode(final String programmeKey, final String credentialsId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("credentialsId", credentialsId)
                .when()
                .post("/simulate/api/factors/{credentialsId}/challenges/latest/get");
    }

    public static Response simulateChallengeExpiry(final String programmeKey,
                                                   final String credentialsId,
                                                   final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("credentialsId", credentialsId)
                .pathParam("challengeId", challengeId)
                .when()
                .post("/simulate/api/factors/{credentialsId}/challenges/{challengeId}/verify_expired");
    }

    public static Response approveOwt(final String programmeKey,
                                      final String owtId,
                                      final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/wiretransfers/outgoing/{owt_id}/accept");
    }

    public static Response rejectOwt(final String programmeKey,
                                     final String owtId,
                                     final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/wiretransfers/outgoing/{owt_id}/reject");
    }

    public static Response acceptAuthyIdentity(final String programmeKey,
                                               final String identityId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("identity_id", identityId)
                .when()
                .post("/simulate/api/authy/enrol/{identity_id}/accept");
    }

    public static Response rejectAuthyIdentity(final String programmeKey,
                                               final String identityId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("identity_id", identityId)
                .when()
                .post("/simulate/api/authy/enrol/{identity_id}/reject");
    }

    public static Response acceptAuthyOwt(final String programmeKey,
                                          final String owtId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/authy/owt/{owt_id}/accept");
    }

    public static Response rejectAuthyOwt(final String programmeKey,
                                          final String owtId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/authy/owt/{owt_id}/reject");
    }

    public static Response acceptAuthySend(final String programmeKey,
                                           final String sendId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("send_id", sendId)
                .when()
                .post("/simulate/api/authy/send/{send_id}/accept");
    }

    public static Response rejectAuthySend(final String programmeKey,
                                           final String sendId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("send_id", sendId)
                .when()
                .post("/simulate/api/authy/send/{send_id}/reject");
    }

    public static Response acceptAuthyStepUp(final String programmeKey,
                                             final String sessionId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("session_id", sessionId)
                .when()
                .post("/simulate/api/authy/step_up/{session_id}/accept");
    }

    public static Response rejectAuthyStepUp(final String programmeKey,
                                             final String sessionId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("session_id", sessionId)
                .when()
                .post("/simulate/api/authy/step_up/{session_id}/reject");
    }

    public static Response enableAuthyLiveNotifications(final String programmeKey) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .when()
                .post("/simulate/api/authy/live/enable");
    }

    public static Response disableAuthyLiveNotifications(final String programmeKey) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .when()
                .post("/simulate/api/authy/live/disable");
    }

    public static Response createMandate(final SimulateCreateMandateModel createMandateModel,
                                         final String programmeKey,
                                         final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .body(createMandateModel)
                .when()
                .post("/simulate/api/directdebits/mandates/create");
    }

    public static Response cancelMandate(final String mandateId,
                                         final String programmeKey,
                                         final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("mandate_id", mandateId)
                .when()
                .post("/simulate/api/directdebits/mandates/{mandate_id}/cancel");
    }

    public static Response expireMandate(final String mandateId,
                                         final String programmeKey,
                                         final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("mandate_id", mandateId)
                .when()
                .post("/simulate/api/directdebits/mandates/{mandate_id}/expire");
    }

    public static Response createMandateCollection(final SimulateCreateCollectionModel createCollectionModel,
                                                   final String mandateId,
                                                   final String programmeKey,
                                                   final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("mandate_id", mandateId)
                .body(createCollectionModel)
                .when()
                .post("/simulate/api/directdebits/mandates/{mandate_id}/collections/create");
    }

    public static Response collectCollection(final String collectionId,
                                             final String mandateId,
                                             final String programmeKey,
                                             final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("mandate_id", mandateId)
                .pathParam("collection_id", collectionId)
                .when()
                .post("/simulate/api/directdebits/mandates/{mandate_id}/collections/{collection_id}/collect");
    }

    public static Response resolveUnpaidCollection(final String collectionId,
                                                   final String mandateId,
                                                   final String programmeKey,
                                                   final String authenticationToken) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .header("Authorization", authenticationToken)
                .pathParam("mandate_id", mandateId)
                .pathParam("collection_id", collectionId)
                .when()
                .post("/simulate/api/directdebits/mandates/{mandate_id}/collections/{collection_id}/resolve_unpaid");
    }

    public static Response simulateEnrolmentLinking(final String programmeKey,
                                                    final String identityId,
                                                    final String linkingCode) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("identity_id", identityId)
                .pathParam("linking_code", linkingCode)
                .when()
                .post("/simulate/api/okay/enrol/{identity_id}/code/{linking_code}/link");
    }

    public static Response simulateOkayUnlinking(final String programmeKey,
                                                 final String identityId,
                                                 final String linkingCode) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("identity_id", identityId)
                .pathParam("linking_code", linkingCode)
                .when()
                .post("/simulate/api/okay/enrol/{identity_id}/code/{linking_code}/unlink");
    }

    public static Response acceptOkayIdentity(final String programmeKey,
                                              final String identityId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("identity_id", identityId)
                .when()
                .post("/simulate/api/okay/enrol/{identity_id}/accept");
    }

    public static Response rejectOkayIdentity(final String programmeKey,
                                              final String identityId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("identity_id", identityId)
                .when()
                .post("/simulate/api/okay/enrol/{identity_id}/reject");
    }

    public static Response acceptOkayOwt(final String programmeKey,
                                         final String owtId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/okay/owt/{owt_id}/accept");
    }

    public static Response rejectOkayOwt(final String programmeKey,
                                         final String owtId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/okay/owt/{owt_id}/reject");
    }

    public static Response acceptOkayThreeDSecureChallenge(final String programmeKey,
                                                           final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .when()
                .post("/simulate/api/okay/three_ds/challenge/{challenge_id}/accept");
    }

    public static Response rejectOkayThreeDSecureChallenge(final String programmeKey,
                                                           final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .when()
                .post("/simulate/api/okay/three_ds/challenge/{challenge_id}/reject");
    }

    public static Response acceptAuthyThreeDSecureChallenge(final String programmeKey,
                                                            final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .when()
                .post("/simulate/api/authy/three_ds/challenge/{challenge_id}/accept");
    }

    public static Response rejectAuthyThreeDSecureChallenge(final String programmeKey,
                                                            final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .when()
                .post("/simulate/api/authy/three_ds/challenge/{challenge_id}/reject");
    }

    public static Response okayOwtPinFallback(
            final String programmeKey,
            final String owtId,
            final SimulatePasscodeModel pin
    ) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("owt_id", owtId)
                .body(pin)
                .when()
                .post("/simulate/api/okay/owt/{owt_id}/pin");
    }

    public static Response acceptOkayLoginChallenge(final String programmeKey,
                                                    final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .when()
                .post("/simulate/api/okay/login/{challenge_id}/accept");
    }

    public static Response enterPinOkayLoginChallenge(final String programmeKey,
                                                      final String challengeId,
                                                      final BiometricPinModel biometricPinModel) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .body(biometricPinModel)
                .when()
                .post("/simulate/api/okay/login/{challenge_id}/pin");
    }

    public static Response rejectOkayLoginChallenge(final String programmeKey,
                                                    final String challengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challengeId)
                .when()
                .post("/simulate/api/okay/login/{challenge_id}/reject");
    }

    public static Response acceptBiometricStepUp(final String programmeKey,
                                                 final String challenge_id) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challenge_id)
                .when()
                .post("/simulate/api/okay/stepup/{challenge_id}/accept");
    }

    public static Response rejectBiometricStepUp(final String programmeKey,
                                                 final String challenge_id) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("challenge_id", challenge_id)
                .when()
                .post("/simulate/api/okay/stepup/{challenge_id}/reject");
    }

    public static Response acceptOkaySend(final String programmeKey,
                                          final String sendId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("send_id", sendId)
                .when()
                .post("/simulate/api/okay/sends/{send_id}/accept");
    }

    public static Response rejectOkaySend(final String programmeKey,
                                          final String sendId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("send_id", sendId)
                .when()
                .post("/simulate/api/okay/sends/{send_id}/reject");
    }

    public static Response okaySendPinFallback(final String programmeKey,
                                               final String sendId,
                                               final SimulatePasscodeModel pin) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("send_id", sendId)
                .body(pin)
                .when()
                .post("/simulate/api/okay/owt/{send_id}/pin");
    }

    public static Response acceptOkayBeneficiaryBatch(final String programmeKey,
                                                      final String batchId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("batch_id", batchId)
                .when()
                .post("/simulate/api/okay/beneficiary_management/{batch_id}/accept");
    }

    public static Response rejectOkayBeneficiaryBatch(final String programmeKey,
                                                      final String batchId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("batch_id", batchId)
                .when()
                .post("/simulate/api/okay/beneficiary_management/{batch_id}/reject");
    }

    public static Response acceptAuthyBeneficiaryBatch(final String programmeKey,
                                                       final String batchId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("batch_id", batchId)
                .when()
                .post("/simulate/api/authy/beneficiary_management/{batch_id}/accept");
    }

    public static Response rejectAuthyBeneficiaryBatch(final String programmeKey,
                                                       final String batchId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("batch_id", batchId)
                .when()
                .post("/simulate/api/authy/beneficiary_management/{batch_id}/reject");
    }

    public static Response okayOwtForgotPin(final String programmeKey,
                                            final String owtId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("owt_id", owtId)
                .when()
                .post("/simulate/api/okay/owt/{owt_id}/forgot_pin");
    }


    public static Response acceptAuthyChallenge(final String programmeKey,
                                                final String scaChallengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("sca_challenge_id", scaChallengeId)
                .when()
                .post("/simulate/api/authy/challenge/{sca_challenge_id}/accept");
    }

    public static Response rejectAuthyChallenge(final String programmeKey,
                                                final String scaChallengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("sca_challenge_id", scaChallengeId)
                .when()
                .post("/simulate/api/authy/challenge/{sca_challenge_id}/reject");
    }

    public static Response acceptOkayChallenge(final String programmeKey,
                                               final String scaChallengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("sca_challenge_id", scaChallengeId)
                .when()
                .post("/simulate/api/okay/challenge/{sca_challenge_id}/accept");
    }

    public static Response rejectOkayChallenge(final String programmeKey,
                                               final String scaChallengeId) {
        return simulatorRestAssured()
                .header("Content-type", "application/json")
                .header("programme-key", programmeKey)
                .pathParam("sca_challenge_id", scaChallengeId)
                .when()
                .post("/simulate/api/okay/challenge/{sca_challenge_id}/reject");
    }

    public static Response setManagedAccountCannedResponse(final SetCannedResponseModel setCannedResponseModel,
                                                           final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .and()
                .body(setCannedResponseModel)
                .when()
                .post("/api/account_processing_simulator/gps_simulator/canned_responses/put");
    }

    public static Response getManagedAccountCannedResponse(final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .when()
                .post("/api/account_processing_simulator/gps_simulator/canned_responses/peek");
    }

    public static Response clearManagedAccountCannedResponse(final String tenantId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .when()
                .post("/api/account_processing_simulator/gps_simulator/canned_responses/clear");
    }

    public static Response scheduledExecuteOwt(final String tenantId,
                                               final String owtId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("id", owtId)
                .when()
                .post("/api/outgoing_wire_transfers/{id}/scheduled_execute");
    }

    public static Response scheduledExecuteTransfer(final String tenantId,
                                                     final String transferId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("id", transferId)
                .when()
                .post("/api/transfers_v2/{id}/scheduled_execute");
    }

    public static Response scheduledExecuteSends(final String tenantId,
                                                 final String sendId) {
        return oldSimulatorRestAssured()
                .header("Content-type", "application/json")
                .header("tenant-id", tenantId)
                .pathParam("id", sendId)
                .when()
                .post("/api/send_v2/{id}/scheduled_execute");
    }

    public static Response simulateAboutToExpire(final String managedCardId,
                                                 final String programmeKey)  {
        return simulatorRestAssured()
            .header("Content-type", "application/json")
            .header("programme-key", programmeKey)
            .pathParam("managed_card_id", managedCardId)
            .when()
            .post("/simulate/api/cards/{managed_card_id}/about_to_expire");
    }

    public static Response simulateExpire(final String managedCardId,
                                          final String programmeKey)  {
        return simulatorRestAssured()
            .header("Content-type", "application/json")
            .header("programme-key", programmeKey)
            .pathParam("managed_card_id", managedCardId)
            .when()
            .post("/simulate/api/cards/{managed_card_id}/expire");
    }

    public static Response simulateRenew(final String managedCardId,
                                         final String programmeKey)  {
        return simulatorRestAssured()
            .header("Content-type", "application/json")
            .header("programme-key", programmeKey)
            .pathParam("managed_card_id", managedCardId)
            .when()
            .post("/simulate/api/cards/{managed_card_id}/renew");
    }
}
