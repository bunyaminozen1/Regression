package opc.junit.admin.innovator;

import java.sql.SQLException;
import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseInnovatorSetup {

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  protected static ProgrammeDetailsModel applicationOne;
  protected static String adminRootUserToken;
  protected static String adminNonRootUserToken;
  protected static String innovatorId;
  protected static String innovatorEmail;
  protected static String innovatorPassword;
  protected static String innovatorToken;
  protected static String programmeId;

  @BeforeAll
  public static void GlobalSetup() throws SQLException {
    applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

    innovatorId = applicationOne.getInnovatorId();
    innovatorEmail = applicationOne.getInnovatorEmail();
    innovatorPassword = applicationOne.getInnovatorPassword();
    innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    programmeId = applicationOne.getProgrammeId();
    adminRootUserToken = AdminService.loginAdmin();
    adminNonRootUserToken = AdminHelper.createNonRootAdminUser();
  }
}
