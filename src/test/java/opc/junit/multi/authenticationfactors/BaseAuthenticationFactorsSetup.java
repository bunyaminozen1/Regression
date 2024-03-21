package opc.junit.multi.authenticationfactors;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.AUTHENTICATION_FACTORS)
public class BaseAuthenticationFactorsSetup {

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  protected static ProgrammeDetailsModel applicationOne;
  protected static ProgrammeDetailsModel applicationTwo;
  protected static ProgrammeDetailsModel applicationThree;
  protected static ProgrammeDetailsModel passcodeApp;
  protected static ProgrammeDetailsModel secondaryScaApp;
  protected static String corporateProfileId;
  protected static String consumerProfileId;
  protected static String secretKey;
  protected static String sharedKey;
  protected static String passcodeAppCorporateProfileId;
  protected static String passcodeAppConsumerProfileId;
  protected static String passcodeAppSecretKey;
  protected static String passcodeAppSharedKey;
  protected static String tenantId;
  protected static String programmeId;
  protected static String programmeName;
  protected static String impersonatedAdminToken;
  protected static String innovatorToken;

  @BeforeAll
  public static void GlobalSetup() {
    applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
    applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
    applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
    passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);
    secondaryScaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SECONDARY_SCA_APP);

    corporateProfileId = applicationOne.getCorporatesProfileId();
    consumerProfileId = applicationOne.getConsumersProfileId();
    secretKey = applicationOne.getSecretKey();
    sharedKey = applicationOne.getSharedKey();

    passcodeAppCorporateProfileId = passcodeApp.getCorporatesProfileId();
    passcodeAppConsumerProfileId = passcodeApp.getConsumersProfileId();
    passcodeAppSecretKey = passcodeApp.getSecretKey();
    passcodeAppSharedKey = passcodeApp.getSharedKey();

    tenantId = applicationOne.getInnovatorId();
    programmeId = applicationOne.getProgrammeId();
    programmeName = applicationOne.getProgrammeName();
    impersonatedAdminToken = AdminService.impersonateTenant(tenantId, AdminService.loginAdmin());
    innovatorToken = InnovatorHelper.loginInnovator(applicationOne.getInnovatorEmail(), applicationOne.getInnovatorPassword());
  }

  protected static String getBackofficeImpersonateToken(final String identityId,
      final IdentityType identityType) {
    return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
  }
}
