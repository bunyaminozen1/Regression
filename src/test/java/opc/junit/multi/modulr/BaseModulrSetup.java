package opc.junit.multi.modulr;

import opc.enums.opc.InnovatorSetup;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseModulrSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel modulrApp;
    protected static ProgrammeDetailsModel payneticsApp;
    protected static ProgrammeDetailsModel passcodeApp;
    protected static String corporateProfileId;
    protected static String payneticsCorporateProfileId;
    protected static String consumerProfileId;
    protected static String payneticsConsumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String payneticsCorporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String payneticsConsumerManagedAccountProfileId;
    protected static String secretKey;
    protected static String payneticsSecretKey;
    protected static String innovatorEmail;
    protected static String payneticsInnovatorEmail;
    protected static String innovatorPassword;
    protected static String payneticsInnovatorPassword;

    protected static String payneticsProgrammeId;
    protected static String payneticsInnovatorId;

    protected static String adminToken;

    @BeforeAll
    public static void GlobalSetup(){

        modulrApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.MODULR_APP);
        payneticsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PAYNETICS_APP);

        innovatorEmail = modulrApp.getInnovatorEmail();
        payneticsInnovatorEmail = payneticsApp.getInnovatorEmail();
        innovatorPassword = modulrApp.getInnovatorPassword();
        payneticsInnovatorPassword = payneticsApp.getInnovatorPassword();

        corporateProfileId = modulrApp.getCorporatesProfileId();
        payneticsCorporateProfileId = payneticsApp.getCorporatesProfileId();
        consumerProfileId = modulrApp.getConsumersProfileId();
        payneticsConsumerProfileId = payneticsApp.getConsumersProfileId();

        corporateManagedAccountProfileId = modulrApp.getCorporateModulrManagedAccountsProfileId();
        payneticsCorporateManagedAccountProfileId = payneticsApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = modulrApp.getConsumerModulrManagedAccountsProfileId();
        payneticsConsumerManagedAccountProfileId = payneticsApp.getConsumerModulrManagedAccountsProfileId();
        secretKey = modulrApp.getSecretKey();
        payneticsSecretKey = payneticsApp.getSecretKey();

        payneticsProgrammeId = payneticsApp.getProgrammeId();
        payneticsInnovatorId = payneticsApp.getInnovatorId();

        adminToken = AdminService.loginAdmin();
    }
}
