package opc.junit.multi.users;

import opc.enums.opc.InnovatorSetup;
import opc.junit.multi.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_OK;

public class BaseUsersSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel applicationThree;
    protected static ProgrammeDetailsModel nonFpsApplication;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String secretKeyAppTwo;
    protected static String innovatorName;
    protected static String adminToken;

    @BeforeAll
    public static void GlobalSetup() throws SQLException {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
        nonFpsApplication = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        secretKey = applicationOne.getSecretKey();

        secretKeyAppTwo = applicationTwo.getSecretKey();
        adminToken = AdminService.loginAdmin();
        innovatorName = AdminService.getInnovator(adminToken, applicationOne.getInnovatorId())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("innovatorName");
    }
}
