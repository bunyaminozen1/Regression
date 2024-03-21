package opc.services.multi;

import io.restassured.response.Response;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.models.multi.beneficiaries.RemoveBeneficiariesModel;
import opc.models.shared.VerificationModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class BeneficiariesService extends BaseService {

  public static Response createBeneficiariesBatch(final CreateBeneficiariesBatchModel createBeneficiariesBatchModel,
                                                  final String secretKey,
                                                  final String token){
    return getBodyApiKeyAuthenticationRequest(createBeneficiariesBatchModel, secretKey, token)
        .when()
        .post("/multi/beneficiaries");
  }

  public static Response getBeneficiaryBatches(final String secretKey,
                                               final Optional<Map<String, Object>> filters,
                                               final String token){
    return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
        .when()
        .get("/multi/beneficiaries/batch");
  }

  public static Response getBeneficiaryBatch(final String batchId,
                                             final String secretKey,
                                             final String token){
    return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
        .pathParam("id", batchId)
        .when()
        .get("/multi/beneficiaries/batch/{id}");
  }

  public static Response getBeneficiaries(final String secretKey,
                                          final Optional<Map<String, Object>> filters,
                                          final String token){
    return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
        .when()
        .get("/multi/beneficiaries");
  }

  public static Response getBeneficiary(final String beneficiaryId,
                                        final String secretKey,
                                        final String token){
    return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
        .pathParam("id", beneficiaryId)
        .when()
        .get("/multi/beneficiaries/{id}");
  }

  public static Response removeBeneficiaries(final RemoveBeneficiariesModel removeBeneficiariesModel,
                                             final String secretKey,
                                             final String token){
    return getBodyApiKeyAuthenticationRequest(removeBeneficiariesModel, secretKey, token)
        .when()
        .post("/multi/beneficiaries/remove");
  }

  public static Response startBeneficiaryBatchPushVerification(final String batchId,
                                                               final String channel,
                                                               final String secretKey,
                                                               final String token){
    return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
        .pathParam("id", batchId)
        .pathParam("channel", channel)
        .when()
        .post("/multi/beneficiaries/batch/{id}/challenges/push/{channel}");
  }

  public static Response startBeneficiaryBatchOtpVerification(final String batchId,
                                                              final String channel,
                                                              final String secretKey,
                                                              final String token) {
    return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
        .pathParam("id", batchId)
        .pathParam("channel", channel)
        .when()
        .post("/multi/beneficiaries/batch/{id}/challenges/otp/{channel}");
  }

  public static Response verifyBeneficiaryBatchOtp(final VerificationModel verificationModel,
                                                   final String batchId,
                                                   final String channel,
                                                   final String secretKey,
                                                   final String token) {
    return getBodyApiKeyAuthenticationRequest(verificationModel, secretKey, token, Optional.empty())
        .pathParam("id", batchId)
        .pathParam("channel", channel)
        .when()
        .post("/multi/beneficiaries/batch/{id}/challenges/otp/{channel}/verify");
  }
}