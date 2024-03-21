package opc.services.backoffice.multi;

import io.restassured.response.Response;
import opc.enums.opc.AcceptedResponse;
import opc.models.backoffice.ImpersonateIdentityModel;
import opc.models.backoffice.SpendRulesModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.FeesChargeModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class BackofficeMultiService extends BaseService {

    public static Response impersonateIdentity(final ImpersonateIdentityModel impersonateIdentityModel,
                                               final String secretKey){
        return getBodyApiKeyRequest(impersonateIdentityModel, secretKey)
                .when()
                .post("/multi/backoffice/impersonate_identity_login");
    }

    public static Response impersonateIdentityAccessToken(final ImpersonateIdentityModel impersonateIdentityModel,
                                               final String secretKey){
        return getBodyApiKeyRequest(impersonateIdentityModel, secretKey)
                .when()
                .post("/multi/backoffice/access_token");
    }

    public static Response getManagedCardsSpendRules(final String secretKey,
                                                     final String managedCardId,
                                                     final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/backoffice/managed_cards/{id}/spend_rules");
    }

    public static Response getManagedCard(final String secretKey,
                                          final String managedCardId,
                                          final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/backoffice/managed_cards/{id}");
    }

    public static Response postManagedCardsSpendRules(final SpendRulesModel spendRulesModel,
                                                      final String secretKey,
                                                      final String managedCardId,
                                                      final String token){
        return getBodyApiKeyAuthenticationRequest(spendRulesModel, secretKey, token)
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/backoffice/managed_cards/{id}/spend_rules");
    }

    public static Response patchManagedCardsSpendRules(final SpendRulesModel spendRulesModel,
                                                       final String secretKey,
                                                       final String managedCardId,
                                                       final String token){
        return getBodyApiKeyAuthenticationRequest(spendRulesModel, secretKey, token)
                .pathParam("id", managedCardId)
                .when()
                .patch("/multi/backoffice/managed_cards/{id}/spend_rules");
    }

    public static Response deleteManagedCardsSpendRules(final String secretKey,
                                                        final String managedCardId,
                                                        final String token){
        return getApiKeyAuthenticationRequest(secretKey, token)
                .pathParam("id", managedCardId)
                .when()
                .delete("/multi/backoffice/managed_cards/{id}/spend_rules");
    }

    public static Response getConsumers(final String secretKey,
                                        final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .when()
                .get("/multi/backoffice/consumers");
    }

    public static Response chargeConsumerFee(final FeesChargeModel feesChargeModel,
                                             final String secretKey,
                                             final String token,
                                             final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(feesChargeModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/backoffice/consumers/fees/charge");
    }
    public static Response chargeCorporateFee(final FeesChargeModel feesChargeModel,
                                              final String secretKey,
                                              final String token,
                                              final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(feesChargeModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/backoffice/corporates/fees/charge");
    }

    public static Response getManagedAccount(final String secretKey,
                                             final String managedAccountId,
                                             final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .get("/multi/backoffice/managed_accounts/{id}");
    }
    public static Response transferFunds(final TransferFundsModel transferFundsModel,
                                         final String secretKey,
                                         final String token,
                                         final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(transferFundsModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/backoffice/transfers");
    }
    public static Response getTransfers(final String secretKey,
                                        final Optional<Map<String, Object>> filters,
                                        final String token){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/backoffice/transfers");
    }
    public static Response getTransfer(final String secretKey,
                                       final String transactionId,
                                       final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", transactionId)
                .when()
                .get("/multi/backoffice/transfers/{id}");
    }
    public static Response blockManagedCard(final String secretKey,
                                            final String managedCardId,
                                            final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/backoffice/managed_cards/{id}/block");
    }
    public static Response unblockManagedCard(final String secretKey,
                                              final String managedCardId,
                                              final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/backoffice/managed_cards/{id}/unblock");
    }

    public static Response removeManagedCard(final String secretKey,
                                             final String managedCardId,
                                             final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/backoffice/managed_cards/{id}/remove");
    }
    public static Response getManagedAccountStatement(final String secretKey,
                                                      final String managedAccountId,
                                                      final String token,
                                                      final Optional<Map<String, Object>> filters,
                                                      final AcceptedResponse acceptedResponse){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .header("accept", acceptedResponse.getAccept())
                .pathParam("id", managedAccountId)
                .when()
                .get("/multi/backoffice/managed_accounts/{id}/statement");
    }
    public static Response getManagedAccountStatement(final String secretKey,
                                                      final String managedAccountId,
                                                      final String token,
                                                      final Optional<Map<String, Object>> filters,
                                                      final Optional<Map<String, Object>> headers){
        return assignHeaderParams(assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters), headers)
                .pathParam("id", managedAccountId)
                .when()
                .get("/multi/backoffice/managed_accounts/{id}/statement");
    }

    public static Response getManagedCardStatement(final String secretKey,
                                                   final String managedCardId,
                                                   final String token,
                                                   final Optional<Map<String, Object>> filters,
                                                   final AcceptedResponse acceptedResponse){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .header("accept", acceptedResponse.getAccept())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/backoffice/managed_cards/{id}/statement");
    }

    public static Response getManagedCardStatement(final String secretKey,
                                                   final String managedCardId,
                                                   final String token,
                                                   final Optional<Map<String, Object>> filters,
                                                   final Optional<Map<String, Object>> headers){
        return assignHeaderParams(assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters), headers)
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/backoffice/managed_cards/{id}/statement");
    }

    public static Response chargeFee(final FeesChargeModel feesChargeModel,
                                             final String secretKey,
                                             final String token,
                                             final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(feesChargeModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/backoffice/fees/charge");
    }
}
