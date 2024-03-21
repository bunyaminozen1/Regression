package opc.junit.mobile;

import opc.enums.opc.KycLevel;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.secure.SetIdentityDetailsModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class SetIdentityDetailsTests extends BaseIdentitySetup {

    @Test
    public void SetAddressRequired_ConsumerWithKycReference_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReference, new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SetAddressNotRequired_ConsumerWithKycReference_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReference, new SetIdentityDetailsModel(true))
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SetAddressRequired_ConsumerWithExternalUserId_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), externalUserId, new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SetAddressNotRequired_ConsumerWithExternalUserId_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), externalUserId, new SetIdentityDetailsModel(true))
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SetAddressRequired_Corporate_NotSupported(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporatesProfileId, secretKey);
        final String kybReference = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        SecureService.setIdentityDetails(sharedKey, corporate.getRight(), kybReference, new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_SUPPORTED"));
    }

    @Test
    public void SetAddressRequired_ConsumerUsingCorporateToken_NotSupported(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporatesProfileId, secretKey);

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.setIdentityDetails(sharedKey, corporate.getRight(), kycReference, new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_SUPPORTED"));
    }

    @Test
    public void SetAddressRequired_ConsumerUsingOtherConsumerToken_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);

        final Pair<String, String> consumer1 = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer1.getRight());

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReference, new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetIdentityDetails_UnknownReferenceId_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), RandomStringUtils.randomAlphabetic(5), new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetIdentityDetails_KycNotStarted_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), RandomStringUtils.randomAlphabetic(5), new SetIdentityDetailsModel(false))
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
