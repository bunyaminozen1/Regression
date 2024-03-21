package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.models.shared.*;
import commons.services.BaseService;

import java.util.Optional;

public class CorporatesService extends BaseService {

    public static Response createCorporate(final CreateCorporateModel createCorporateModel,
                                           final String secretKey,
                                           final Optional<String> idempotencyRef){
        return getBodyApiKeyRequest(createCorporateModel, secretKey, idempotencyRef)
                .when()
                .post("/multi/corporates");
    }

    public static Response getCorporates(final String secretKey,
                                         final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .get("/multi/corporates");
    }

    public static Response patchCorporate(final PatchCorporateModel patchCorporateModel,
                                          final String secretKey,
                                          final String token,
                                          final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(patchCorporateModel, secretKey, token, idempotencyRef)
                .when()
                .patch("/multi/corporates");
    }

    public static Response sendEmailVerification(final SendEmailVerificationModel sendEmailVerificationModel,
                                                 final String secretKey){
        return getBodyApiKeyRequest(sendEmailVerificationModel, secretKey)
                .when()
                .post("/multi/corporates/verification/email/send");
    }

    public static Response verifyEmail(final EmailVerificationModel emailVerificationModel,
                                       final String secretKey){
        return getBodyApiKeyRequest(emailVerificationModel, secretKey)
                .when()
                .post("/multi/corporates/verification/email/verify");
    }

    public static Response sendMobileVerification(final String secretKey,
                                                  final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .post("/multi/corporates/verification/mobile/send");
    }

    public static Response verifyMobile(final MobileVerificationModel mobileVerificationModel,
                                        final String secretKey,
                                        final String token){
        return getBodyApiKeyAuthenticationRequest(mobileVerificationModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/corporates/verification/mobile/verify");
    }

    public static Response startCorporateKyb(final String secretKey,
                                             final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .post("/multi/corporates/kyb");
    }

    public static Response getCorporateKyb(final String secretKey,
                                           final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .get("/multi/corporates/kyb");
    }

    public static Response chargeCorporateFee(final FeesChargeModel feesChargeModel,
                                              final String secretKey,
                                              final String token,
                                              final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(feesChargeModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/corporates/fees/charge");
    }

    public static Response enrolCorporate(final EnrolRootModel enrolRootModel,
                                          final String secretKey,
                                          final String token){
        return getBodyApiKeyAuthenticationRequest(enrolRootModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/corporates/factors");
    }

    public static Response verifyCorporateEnrolment(final VerifyEnrolmentModel verifyEnrolmentModel,
                                                    final String factorType,
                                                    final String secretKey,
                                                    final String token){
        return getBodyApiKeyAuthenticationRequest(verifyEnrolmentModel, secretKey, token, Optional.empty())
                .pathParam("factor_type", factorType)
                .when()
                .post("/multi/corporates/factors/{factor_type}/verify");
    }
}