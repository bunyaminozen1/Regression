package opc.junit.mobile;

import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetIdentityStateTests extends BaseIdentitySetup {

    @Test
    public void GetIdentityState_ConsumerInitiatedWithKycReference_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(true))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerPendingReviewWithKycReference_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        ConsumersDatabaseHelper.updateConsumerKyc(KycState.PENDING_REVIEW.name(), consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerRejectedWithKycReference_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        ConsumersDatabaseHelper.updateConsumerKyc(KycState.REJECTED.name(), consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerNotStartedWithKycReference_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        ConsumersDatabaseHelper.updateConsumerKyc(KycState.NOT_STARTED.name(), consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerApprovedWithKycReference_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerInitiatedWithExternalUserId_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(true))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerPendingReviewWithExternalUserId_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        ConsumersDatabaseHelper.updateConsumerKyc(KycState.PENDING_REVIEW.name(), consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerRejectedWithExternalUserId_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        ConsumersDatabaseHelper.updateConsumerKyc(KycState.REJECTED.name(), consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerNotStartedWithExternalUserId_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        ConsumersDatabaseHelper.updateConsumerKyc(KycState.NOT_STARTED.name(), consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_ConsumerApprovedWithExternalUserId_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(false))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void GetIdentityState_Corporate_NotSupported(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporatesProfileId, secretKey);
        final String kybReference = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        SecureService.getIdentityState(sharedKey, corporate.getRight(), kybReference)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_SUPPORTED"));
    }

    @Test
    public void GetIdentityState_ConsumerUsingCorporateToken_Unauthorised(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporatesProfileId, secretKey);

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.getIdentityState(sharedKey, corporate.getRight(), kycReference)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_SUPPORTED"));
    }

    @Test
    public void GetIdentityState_ConsumerUsingOtherConsumerToken_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);

        final Pair<String, String> consumer1 = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer1.getRight());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
