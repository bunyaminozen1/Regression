package opc.junit.innovator.templates;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class BaseEmailTemplateSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String corporateProfileId;
    protected static String consumerProfileId;

    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;

    protected static String programmeId;

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        programmeId = applicationOne.getProgrammeId();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }
}