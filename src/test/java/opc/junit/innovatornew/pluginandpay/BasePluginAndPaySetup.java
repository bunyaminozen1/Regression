package opc.junit.innovatornew.pluginandpay;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.admin.CreatePluginModel;
import opc.models.admin.CreatePluginResponseModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BasePluginAndPaySetup {

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  protected static String corporateProfileId;
  protected static ProgrammeDetailsModel applicationOne;
  protected static ProgrammeDetailsModel webhooksApp;
  protected static String innovatorEmail;
  protected static String innovatorPassword;
  protected static String innovatorToken;
  protected static String adminToken;
  protected static Long pluginId;
  protected static String tenantId;
  protected static String tenantAdminToken;
  protected static String secretKey;

  @BeforeAll
  public static void GlobalSetup() {

    applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

    innovatorEmail = applicationOne.getInnovatorEmail();
    innovatorPassword = applicationOne.getInnovatorPassword();
    innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    adminToken = AdminService.loginAdmin();
    tenantId = applicationOne.getInnovatorId();
    tenantAdminToken = AdminService.impersonateTenant(tenantId, adminToken);
    corporateProfileId = applicationOne.getCorporatesProfileId();
    secretKey = applicationOne.getSecretKey();

  }

  protected static Long getPluginResponse (CreatePluginModel createPluginModel){

    final CreatePluginResponseModel createPluginResponseModel =
            AdminHelper.createPlugin(createPluginModel, AdminService.loginAdmin());

    pluginId = createPluginResponseModel.getId();
    return pluginId;
  }
}