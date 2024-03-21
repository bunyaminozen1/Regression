package opc.junit.innovator.invitenewinnovator;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class BaseInnovatorSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }
}
