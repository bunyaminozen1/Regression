package opc.junit.adminnew.owtprofile;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseOwtProfileSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String secretKey;
    protected static String programmeId;
    protected static String owtProfileId;
    protected static String adminToken;
    protected static String adminTenantToken;

    @BeforeAll
    public static void Setup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        secretKey = applicationOne.getSecretKey();
        programmeId = applicationOne.getProgrammeId();
        owtProfileId = applicationOne.getOwtProfileId();
        adminToken = AdminService.loginAdmin();
        adminTenantToken = AdminService.impersonateTenant(applicationOne.getInnovatorId(), adminToken);
    }
}
