package opc.junit.adminnew.reports;

import opc.enums.opc.PermissionType;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.models.innovator.GetReportsModel;
import opc.services.adminnew.AdminService;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

class GetReportsTests extends BaseReportsSetup {

    private static final String PERMISSION_NAME = "managedAccountsInsights:readManagedAccountsOutgoingTransfers";
    private static final String PASSWORD = "Password1234!";

    @Test
    void GetReports_NoPermissions_NoReports(){
        AdminService.getReports(GetReportsModel.defaultGetReportsModel(), adminTenantImpersonationToken)
                .then()
                .statusCode(SC_OK)
                .body("report", nullValue())
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    void GetReports_OneAdminPermission_OneReport() {
        final var permissionId = AdminHelper.getPermissionId(adminToken, PermissionType.SYSTEM, PERMISSION_NAME);
        final var roleId = AdminHelper.createRole(adminToken, adminCheckerToken, permissionId);
        final var createdAdminToken = AdminHelper.createAdminWithRole(adminToken, PASSWORD, Long.parseLong(roleId));
        final var impersonationToken = opc.services.admin.AdminService.impersonateTenant(innovatorId, createdAdminToken);

        AdminService.getReports(GetReportsModel.defaultGetReportsModel(), impersonationToken)
                .then()
                .statusCode(SC_OK)
                .body("report[0].id", equalTo("65"))
                .body("report[0].title", equalTo("Managed Accounts Outgoing Transfers"))
                .body("report[0].category", equalTo("MANAGED_ACCOUNTS"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }
}
