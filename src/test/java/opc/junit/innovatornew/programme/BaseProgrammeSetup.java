package opc.junit.innovatornew.programme;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseProgrammeSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String tenantId;
    protected static String adminToken;
    protected static String tenantAdminToken;
    protected static String innovatorToken;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String programmeId;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        adminToken = AdminService.loginAdmin();
        tenantId = applicationOne.getInnovatorId();
        tenantAdminToken = AdminService.impersonateTenant(tenantId, adminToken);
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        programmeId = applicationOne.getProgrammeId();
    }
}
