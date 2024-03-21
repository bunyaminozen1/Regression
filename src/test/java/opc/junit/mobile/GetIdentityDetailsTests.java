package opc.junit.mobile;

import opc.enums.opc.CompanyType;
import opc.enums.opc.KycLevel;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetIdentityDetailsTests extends BaseIdentitySetup {

    @Test
    public void GetIdentityDetails_ConsumerWithKycReference_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.getIdentityDetails(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("params.identityType", equalTo("consumers"))
                .body("params.externalUserId", notNullValue())
                .body("params.verificationFlow", equalTo("consumer-kyc-flow"))
                .body("params.accessToken", notNullValue())
                .body("params.kycProviderKey", equalTo("sumsub"));
    }

    @Test
    public void GetIdentityDetails_ConsumerWithExternalUserId_Success(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        SecureService.getIdentityDetails(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_OK)
                .body("params.identityType", equalTo("consumers"))
                .body("params.externalUserId", notNullValue())
                .body("params.verificationFlow", equalTo("consumer-kyc-flow"))
                .body("params.accessToken", notNullValue())
                .body("params.kycProviderKey", equalTo("sumsub"));
    }

    @Test
    public void GetIdentityDetails_Corporate_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setType(CompanyType.LLC.name())
                                .build())
                        .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kycReference = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        SecureService.getIdentityDetails(sharedKey, corporate.getRight(), kycReference)
                .then()
                .statusCode(SC_OK)
                .body("params.identityType", equalTo("corporates"))
                .body("params.externalUserId", notNullValue())
                .body("params.verificationFlow", equalTo("corporate-kyb-flow"))
                .body("params.accessToken", notNullValue())
                .body("params.kybProviderKey", equalTo("sumsub"));
    }

    @Test
    public void GetIdentityDetails_ConsumerUsingCorporateToken_NotFound(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporatesProfileId, secretKey);

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.getIdentityDetails(sharedKey, corporate.getRight(), kycReference)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetIdentityDetails_ConsumerWithKycReferenceUsingOtherConsumerToken_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);

        final Pair<String, String> consumer1 = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String kycReference = ConsumersHelper.startKyc(secretKey, consumer1.getRight());

        SecureService.getIdentityDetails(sharedKey, consumer.getRight(), kycReference)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetIdentityDetails_ConsumerWithExternalUserIdUsingOtherConsumerToken_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);

        final Pair<String, String> consumer1 = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        final String externalUserId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer1.getRight());

        SecureService.getIdentityDetails(sharedKey, consumer.getRight(), externalUserId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetIdentityDetails_ConsumerUnknownReferenceId_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);
        ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        SecureService.getIdentityDetails(sharedKey, consumer.getRight(), RandomStringUtils.randomAlphabetic(5))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetIdentityDetails_CorporateUnknownReferenceId_NotFound(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporatesProfileId, secretKey);
        CorporatesHelper.startKyb(secretKey, corporate.getRight());

        SecureService.getIdentityDetails(sharedKey, corporate.getRight(), RandomStringUtils.randomNumeric(5))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetIdentityDetails_KycNotStarted_NotFound(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumersProfileId, secretKey);

        SecureService.getIdentityDetails(sharedKey, consumer.getRight(), RandomStringUtils.randomAlphabetic(5))
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
