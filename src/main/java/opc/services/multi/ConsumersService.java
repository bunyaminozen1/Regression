package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.FeesChargeModel;
import opc.models.shared.MobileVerificationModel;
import opc.models.shared.SendEmailVerificationModel;
import commons.services.BaseService;

import java.util.Optional;

public class ConsumersService extends BaseService {

    public static Response createConsumer(final CreateConsumerModel createConsumerModel,
                                           final String secretKey,
                                          final Optional<String> idempotencyRef){
        return getBodyApiKeyRequest(createConsumerModel, secretKey, idempotencyRef)
                .when()
                .post("/multi/consumers");
    }

    public static Response getConsumers(final String secretKey,
                                        final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .get("/multi/consumers");
    }

    public static Response patchConsumer(final PatchConsumerModel patchConsumerModel,
                                         final String secretKey,
                                         final String token,
                                         final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(patchConsumerModel, secretKey, token, idempotencyRef)
                .when()
                .patch("/multi/consumers");
    }

    public static Response sendEmailVerification(final SendEmailVerificationModel sendEmailVerificationModel,
                                                 final String secretKey){
        return getBodyApiKeyRequest(sendEmailVerificationModel, secretKey)
                .when()
                .post("/multi/consumers/verification/email/send");
    }

    public static Response verifyEmail(final EmailVerificationModel emailVerificationModel,
                                       final String secretKey){
        return getBodyApiKeyRequest(emailVerificationModel, secretKey)
                .when()
                .post("/multi/consumers/verification/email/verify");
    }

    public static Response sendMobileVerification(final String secretKey,
                                                  final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .post("/multi/consumers/verification/mobile/send");
    }

    public static Response verifyMobile(final MobileVerificationModel mobileVerificationModel,
                                        final String secretKey,
                                        final String token){
        return getBodyApiKeyAuthenticationRequest(mobileVerificationModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/consumers/verification/mobile/verify");
    }

    public static Response startConsumerKyc(final StartKycModel startKycModel,
                                            final String secretKey,
                                            final String token){
        return getBodyApiKeyAuthenticationRequest(startKycModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/consumers/kyc");
    }

    public static Response getConsumerKyc(final String secretKey,
                                          final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .get("/multi/consumers/kyc");
    }

    public static Response chargeConsumerFee(final FeesChargeModel feesChargeModel,
                                             final String secretKey,
                                             final String token,
                                             final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(feesChargeModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/consumers/fees/charge");
    }

    public static Response startKycMobile(final StartKycModel startKycModel,
                                          final String secretKey,
                                          final String token){
        return getBodyApiKeyAuthenticationRequest(startKycModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/consumers/kyc_mobile_sumsub");
    }
}