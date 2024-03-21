package opc.junit.adminnew.consumers;

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
    protected static ProgrammeDetailsModel applicationTwo;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String managedAccountProfileId;
    protected static String prepaidCardProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String tenantId;
    protected static String programmeId;
    protected static String sharedKey;
    protected static String adminToken;
    protected static String impersonatedAdminToken;
    protected static String innovatorToken;

    @BeforeAll
    public static void GlobalSetup(){
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        tenantId = applicationOne.getInnovatorId();

        programmeId = applicationOne.getProgrammeId();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        managedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        prepaidCardProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        transfersProfileId = applicationOne.getTransfersProfileId();

        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();

        adminToken = AdminService.loginAdmin();
        impersonatedAdminToken = AdminService.impersonateTenant(tenantId, adminToken);
        innovatorToken = InnovatorHelper.loginInnovator(applicationOne.getInnovatorEmail(), applicationOne.getInnovatorPassword());
    }

    protected static String getBackofficeImpersonateToken(final String email, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(email, identityType, secretKey);
    }
}
