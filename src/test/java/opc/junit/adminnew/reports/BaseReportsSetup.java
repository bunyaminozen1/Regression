package opc.junit.adminnew.reports;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

class BaseReportsSetup {

    protected static String adminTenantImpersonationToken;
    protected static String adminToken;
    protected static String adminCheckerToken;
    protected static String innovatorId;

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    @BeforeAll
    public static void Setup(){

        final ProgrammeDetailsModel applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorId = applicationOne.getInnovatorId();

        adminTenantImpersonationToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());
        adminToken = opc.services.adminnew.AdminService.loginAdmin();
        adminCheckerToken = opc.services.adminnew.AdminService.loginChecker();
    }
}
