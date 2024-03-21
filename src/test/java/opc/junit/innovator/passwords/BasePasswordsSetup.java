package opc.junit.innovator.passwords;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BasePasswordsSetup {

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel passcodeApp;

    protected static String secretKeyAppOne;
    protected static String secretKeyAppTwo;
    protected static String programmeIdAppOne;
    protected static String programmeIdAppTwo;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String corporateProfileIdAppOne;
    protected static String corporateProfileIdAppTwo;
    protected static String consumerProfileIdAppOne;
    protected static String consumerProfileIdAppTwo;
    protected static String adminToken;

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    @BeforeAll
    public static void Setup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        corporateProfileIdAppOne = applicationOne.getCorporatesProfileId();
        corporateProfileIdAppTwo = applicationTwo.getCorporatesProfileId();
        consumerProfileIdAppOne = applicationOne.getConsumersProfileId();
        consumerProfileIdAppTwo = applicationTwo.getConsumersProfileId();

        secretKeyAppOne = applicationOne.getSecretKey();
        secretKeyAppTwo = applicationTwo.getSecretKey();
        programmeIdAppOne = applicationOne.getProgrammeId();
        programmeIdAppTwo = applicationTwo.getProgrammeId();

        adminToken = AdminService.loginAdmin();
    }
}
