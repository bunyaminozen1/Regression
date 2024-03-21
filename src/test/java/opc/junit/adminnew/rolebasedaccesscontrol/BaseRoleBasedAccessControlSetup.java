package opc.junit.adminnew.rolebasedaccesscontrol;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseRoleBasedAccessControlSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();
    protected static ProgrammeDetailsModel applicationOne;
    protected static String adminToken;
    protected static String adminEmail;
    protected static String programmeId;
    protected static String tenantId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        adminToken = AdminService.loginAdmin();
        adminEmail = RandomStringUtils.randomAlphabetic(5) + "@weavr.io";
        tenantId = applicationOne.getInnovatorId();
        programmeId = applicationOne.getProgrammeId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
    }
}
