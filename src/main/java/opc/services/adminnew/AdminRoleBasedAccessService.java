package opc.services.adminnew;

import commons.services.BaseService;
import io.restassured.response.Response;
import opc.models.admin.AcceptInviteModel;
import opc.models.admin.AssignAdminsToRoleModel;
import opc.models.admin.CreateRoleModel;
import opc.models.admin.InviteUserModel;

public class AdminRoleBasedAccessService extends BaseService {

    public static Response createRole(final CreateRoleModel roleModel,
                                      final String token) {
        return getBodyAuthenticatedRequest(roleModel, token)
                .when()
                .post("/admin_new/access_control/roles");
    }

    public static Response updateRole(final CreateRoleModel roleModel,
                                      final String token,
                                      final Long roleId) {
        return getBodyAuthenticatedRequest(roleModel, token)
                .when()
                .pathParam("roleId", roleId)
                .patch("/admin_new/access_control/roles/{roleId}");
    }

    public static Response deleteRole(final Long roleId,
                                      final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .pathParam("roleId", roleId)
                .delete("/admin_new/access_control/roles/{roleId}");
    }

    public static Response retrieveAllRoles(final String token,
                                            final Long adminId) {
        return getAuthenticatedRequest(token)
                .when()
                .pathParam("adminId", adminId)
                .get("/admin_new/gateway/admins/{adminId}/roles");
    }

    public static Response getRoleById(final String token,
                                       final Long roleId) {
        return getAuthenticatedRequest(token)
                .when()
                .pathParam("roleId", roleId)
                .get("/admin_new/access_control/roles/{roleId}");
    }

    public static Response getAllRoles(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get("/admin_new/access_control/roles");
    }

    public static Response assignRole(final AssignAdminsToRoleModel roleModel,
                                      final String token,
                                      final String roleId) {
        return getBodyAuthenticatedRequest(roleModel, token)
                .when()
                .pathParam("roleId", roleId)
                .post("/admin_new/access_control/roles/{roleId}/assignees");
    }

    public static Response unassignRole(final String token,
                                        final Long adminId,
                                        final Long roleId) {
        return getAuthenticatedRequest(token)
                .when()
                .pathParam("adminId", adminId)
                .pathParam("roleId", roleId)
                .delete("/admin_new/access_control/roles/{roleId}/assignees/{adminId}");
    }

    public static Response addPermissionToRole(final Long roleId,
                                               final Long permission,
                                               final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("roleId", roleId)
                .pathParam("permission", permission)
                .put("/admin_new/access_control/roles/{roleId}/permissions/{permission}");
    }

    public static Response deletePermissionToRole(final Long roleId,
                                                  final Long permission,
                                                  final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("roleId", roleId)
                .pathParam("permission", permission)
                .put("/admin_new/access_control/roles/{roleId}/permission/{permission}");
    }

    public static Response getUsersAssignedToRole(final String token,
                                                  final Long roleId) {
        return getAuthenticatedRequest(token)
                .when()
                .pathParam("role_id", roleId)
                .get("/admin_new/gateway/roles/{role_id}/admins");
    }

    public static Response inviteUser(final String adminToken,
                                      final InviteUserModel inviteUserModel) {
        return getBodyAuthenticatedRequest(inviteUserModel, adminToken)
                .when()
                .post("/admin/api/gateway/invites/create");
    }

    public static Response acceptInvite(final AcceptInviteModel acceptInviteModel,
                                        final String inviteId) {
        return getBodyRequest(acceptInviteModel)
                .when()
                .pathParam("invite_id", inviteId)
                .post("/admin/api/gateway/invites/{invite_id}/consume");
    }

    public static Response forceBootstrap(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .post("/admin/api/gateway/force_bootstrap");
    }

    public static Response getAllAdmins(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get("/admin_new/gateway/admins");
    }
}