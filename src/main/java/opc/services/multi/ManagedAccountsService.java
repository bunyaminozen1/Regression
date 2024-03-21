package opc.services.multi;

import io.restassured.response.Response;
import opc.enums.opc.AcceptedResponse;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedaccounts.PatchManagedAccountModel;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class ManagedAccountsService extends BaseService {

    public static Response getManagedAccounts(final String secretKey,
                                              final Optional<Map<String, Object>> filters,
                                              final String token){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/managed_accounts");
    }

    public static Response createManagedAccount(final CreateManagedAccountModel createManagedAccountModel,
                                                final String secretKey,
                                                final String token,
                                                final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(createManagedAccountModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/managed_accounts");
    }

    public static Response getManagedAccount(final String secretKey,
                                             final String managedAccountId,
                                             final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .get("/multi/managed_accounts/{id}");
    }

    public static Response patchManagedAccount(final PatchManagedAccountModel patchManagedAccountModel,
                                               final String secretKey,
                                               final String managedAccountId,
                                               final String token){
        return getBodyApiKeyAuthenticationRequest(patchManagedAccountModel, secretKey, token,Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .patch("/multi/managed_accounts/{id}");
    }

    public static Response assignManagedAccountIban(final String secretKey,
                                                    final String managedAccountId,
                                                    final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .post("/multi/managed_accounts/{id}/iban");
    }

    public static Response getManagedAccountIban(final String secretKey,
                                                 final String managedAccountId,
                                                 final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .get("/multi/managed_accounts/{id}/iban");
    }

    public static Response blockManagedAccount(final String secretKey,
                                               final String managedAccountId,
                                               final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .post("/multi/managed_accounts/{id}/block");
    }

    public static Response unblockManagedAccount(final String secretKey,
                                                 final String managedAccountId,
                                                 final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .post("/multi/managed_accounts/{id}/unblock");
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
                .get("/multi/managed_accounts/{id}/statement");
    }

    public static Response getManagedAccountStatement(final String secretKey,
                                                      final String managedAccountId,
                                                      final String token,
                                                      final Optional<Map<String, Object>> filters,
                                                      final Optional<Map<String, Object>> headers){
        return assignHeaderParams(assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters), headers)
                .pathParam("id", managedAccountId)
                .when()
                .get("/multi/managed_accounts/{id}/statement");
    }

    public static Response removeManagedAccount(final String secretKey,
                                                final String managedAccountId,
                                                final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedAccountId)
                .when()
                .post("/multi/managed_accounts/{id}/remove");
    }
}
