package opc.services.multiprivate;

import commons.services.BaseService;
import io.restassured.response.Response;
import opc.models.admin.AssignRoleModel;
import opc.models.multi.outgoingwiretransfers.CancelOutgoingWireTransferModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multiprivate.RegisterLinkedAccountModel;
import opc.models.multiprivate.SendWithdrawalModel;
import opc.models.multiprivate.SetActiveStateLinkedAccountModel;

import java.util.Optional;

public class MultiPrivateService extends BaseService {

    public static Response validateAccessToken(final String token,
                                               final String secretKey) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .when()
                .post("/multi_private/token/validate");
    }

    public static Response getPluginRoles(final String secretKey,
                                          final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .params("plugin", "PAYMENT_RUN")
                .when()
                .get("/multi_private/roles");
    }

    public static Response getRoleAssignees(final String secretKey,
                                            final String token,
                                            final Long roleId) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .when()
                .pathParam("role_id", roleId)
                .get("/multi_private/roles/{role_id}/assignees");
    }

    public static Response getUserPermissions(final String secretKey,
                                              final String userId,
                                              final String token) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .params("plugin", "PAYMENT_RUN")
                .when()
                .pathParam("user_id", userId)
                .get("/multi_private/users/{user_id}/permissions");
    }

    public static Response getUserRoles(final String secretKey,
                                        final String token,
                                        final String userId) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .params("plugin", "PAYMENT_RUN")
                .when()
                .pathParam("user_id", userId)
                .get("/multi_private/users/{user_id}/roles");

    }

    public static Response assignRole(final String secretKey,
                                      final String token,
                                      final String userId,
                                      final AssignRoleModel assignRoleModel) {
        return getBodyApiKeyAuthenticationRequest(assignRoleModel, secretKey, token)
                .when()
                .pathParam("user_id", userId)
                .post("/multi_private/users/{user_id}/roles");
    }

    public static Response setActiveStateLinkedAccount(final String corporateLinkedAccountId,
                                                       final SetActiveStateLinkedAccountModel setActiveStateLinkedAccountModel,
                                                       final String fpiKey){

        return getBodyFpiKeyRequest(setActiveStateLinkedAccountModel, fpiKey)
                .when()
                .pathParam("corporate_linked_account_id", corporateLinkedAccountId)
                .patch("/multi_private/plugins/linked_accounts/{corporate_linked_account_id}");
    }

    public static Response unassignRole(final String secretKey,
                                        final String token,
                                        final String userId,
                                        final Long roleId) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .when()
                .pathParam("user_id", userId)
                .pathParam("role_id", roleId)
                .delete("/multi_private/users/{user_id}/roles/{role_id}");
    }

    public static Response createLinkedAccount(final RegisterLinkedAccountModel registerLinkedAccountModel,
                                               final String fpiKey) {
        return getBodyFpiKeyRequest(registerLinkedAccountModel, fpiKey)
                .when()
                .post("/multi_private/plugins/linked_accounts");
    }

    public static Response sendWithdrawal(final SendWithdrawalModel sendWithdrawalModel,
                                          final String fpiKey) {
        return getBodyFpiKeyRequest(sendWithdrawalModel, fpiKey)
                .when()
                .post("/multi_private/plugins/withdrawals");
    }

    public static Response sendOutgoingWireTransfer(final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                    final String secretKey,
                                                    final String token,
                                                    final Optional<String> idempotencyRef) {
        return getBodyApiKeyAuthenticationRequest(outgoingWireTransfersModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi_private/outgoing_wire_transfers");
    }

    public static Response retryDeferredOwt(final String secretKey,
                                            final String token,
                                            final String owt_id) {
        return getApiKeyAuthenticationRequest(secretKey, token)
                .when()
                .pathParam("owt_id", owt_id)
                .put("/multi_private/outgoing_wire_transfers/{owt_id}/execute_deferred");
    }

    public static Response cancelOWT (final CancelOutgoingWireTransferModel cancelOutgoingWireTransferModel,
                                      final String secretKey,
                                      final String token) {
        return getBodyApiKeyAuthenticationRequest(cancelOutgoingWireTransferModel, secretKey, token)
                .when()
                .post("/multi_private/outgoing_wire_transfers/cancel");
    }
}