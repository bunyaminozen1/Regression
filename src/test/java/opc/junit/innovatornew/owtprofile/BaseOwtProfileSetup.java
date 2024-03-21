package opc.junit.innovatornew.owtprofile;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
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
    protected static String innovatorToken;

    @BeforeAll
    public static void Setup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        secretKey = applicationOne.getSecretKey();
        programmeId = applicationOne.getProgrammeId();
        owtProfileId = applicationOne.getOwtProfileId();
        innovatorToken = InnovatorHelper.loginInnovator(applicationOne.getInnovatorEmail(), applicationOne.getInnovatorPassword());
    }
}
