package opc.junit.innovatornew.consumers;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseConsumersSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String consumerProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String tenantId;
    protected static String adminToken;
    protected static String impersonatedAdminToken;

    @BeforeAll
    public static void GlobalSetup(){
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        tenantId = applicationOne.getInnovatorId();

        consumerProfileId = applicationOne.getConsumersProfileId();

        secretKey = applicationOne.getSecretKey();

        adminToken = AdminService.loginAdmin();
        impersonatedAdminToken = AdminService.impersonateTenant(tenantId, adminToken);
    }

    protected static String getBackofficeImpersonateToken(final String email, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(email, identityType, secretKey);
    }
}