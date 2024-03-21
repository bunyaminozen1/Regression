package opc.junit.admin.authenticatorfactors;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.admin.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseAuthenticationFactorsSetup {

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  protected static ProgrammeDetailsModel applicationOne;
  protected static ProgrammeDetailsModel applicationThree;
  protected static String corporateProfileId;
  protected static String consumerProfileId;
  protected static String secretKey;

  @BeforeAll
  public static void GlobalSetup() {
    applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(
        InnovatorSetup.APPLICATION_ONE);
    corporateProfileId = applicationOne.getCorporatesProfileId();
    consumerProfileId = applicationOne.getConsumersProfileId();

    secretKey = applicationOne.getSecretKey();
  }

  protected static String getBackofficeImpersonateToken(final String identityId,
      final IdentityType identityType) {
    return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
  }
}
