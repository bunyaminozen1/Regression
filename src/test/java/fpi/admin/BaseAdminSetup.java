package fpi.admin;

import fpi.paymentrun.BaseSetupExtension;
import fpi.paymentrun.InnovatorSetup;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.PluginsTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag(PluginsTags.PLUGINS_ADMIN_PORTAL)
@Execution(ExecutionMode.CONCURRENT)
public class BaseAdminSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel pluginsApp;
    protected static ProgrammeDetailsModel pluginsAppTwo;
    protected static String programmeId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String corporateProfileId;
    protected static String linkedManagedAccountProfileId;
    protected static String zeroBalanceManagedAccountProfileId;
    protected static String paymentOwtProfileId;
    protected static String innovatorName;
    protected static String sweepOwtProfileId;

    protected static String adminToken;

    protected static String programmeIdAppTwo;
    protected static String corporateProfileIdAppTwo;

    @BeforeAll
    public static void GlobalSetup() {
        pluginsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_APP);
        pluginsAppTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_APP_TWO);

        programmeId = pluginsApp.getProgrammeId();
        secretKey = pluginsApp.getSecretKey();
        sharedKey = pluginsApp.getSharedKey();
        corporateProfileId = pluginsApp.getCorporatesProfileId();
        linkedManagedAccountProfileId = pluginsApp.getLinkedAccountsProfileId();
        zeroBalanceManagedAccountProfileId = pluginsApp.getZeroBalanceManagedAccountsProfileId();
        paymentOwtProfileId = pluginsApp.getPaymentOwtProfileId();
        innovatorName = pluginsApp.getInnovatorName();
        sweepOwtProfileId = pluginsApp.getSweepOwtProfileId();

        adminToken = AdminService.loginAdmin();

        programmeIdAppTwo = pluginsAppTwo.getProgrammeId();
        corporateProfileIdAppTwo = pluginsAppTwo.getCorporatesProfileId();
    }

}
