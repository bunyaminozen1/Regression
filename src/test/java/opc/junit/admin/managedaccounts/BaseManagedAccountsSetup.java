package opc.junit.admin.managedaccounts;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseManagedAccountsSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String secretKey;
    protected static String adminToken;
    protected static String programmeId;

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporateManagedAccountsProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKey = applicationOne.getSecretKey();
        programmeId = applicationOne.getProgrammeId();

        adminToken = AdminService.loginAdmin();
    }
}