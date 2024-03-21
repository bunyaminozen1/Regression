package opc.junit.innovator.reports;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseReportsSetup {

    protected final static String TOP_REPORT_ID = "65";
    protected static String innovatorToken;

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    @BeforeAll
    public static void Setup(){

        final ProgrammeDetailsModel applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        final String innovatorEmail = applicationOne.getInnovatorEmail();
        final String innovatorPassword = applicationOne.getInnovatorPassword();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }
}
