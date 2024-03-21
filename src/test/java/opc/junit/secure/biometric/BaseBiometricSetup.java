package opc.junit.secure.biometric;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.SecureTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag(SecureTags.SECURE)
@Execution(ExecutionMode.CONCURRENT)
public class BaseBiometricSetup {

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  protected static ProgrammeDetailsModel passcodeApp;
  protected static ProgrammeDetailsModel applicationTwo;
  protected static ProgrammeDetailsModel applicationThree;
  protected static ProgrammeDetailsModel applicationFour;
  protected static ProgrammeDetailsModel scaMcApp;
  protected static ProgrammeDetailsModel applicationOne;
  protected static String corporateProfileId;
  protected static String consumerProfileId;
  protected static String corporateManagedAccountProfileId;
  protected static String consumerManagedAccountProfileId;
  protected static String corporatePrepaidManagedCardProfileId;
  protected static String consumerPrepaidManagedCardProfileId;
  protected static String corporateDebitManagedCardProfileId;
  protected static String transfersProfileId;
  protected static String sendsProfileId;
  protected static String outgoingWireTransfersProfileId;
  protected static String secretKey;
  protected static String sharedKey;
  protected static String innovatorId;
  protected static String innovatorEmail;
  protected static String innovatorPassword;
  protected static String innovatorToken;
  protected static Pair<String, String> webhookServiceDetails;
  protected static String adminImpersonatedTenantToken;
  protected static String programmeId;

  @BeforeAll
  public static void GlobalSetup() {

    passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);
    applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
    applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
    applicationFour = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_FOUR);
    applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
    scaMcApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_MC_APP);

    innovatorId = passcodeApp.getInnovatorId();
    innovatorEmail = applicationFour.getInnovatorEmail();
    innovatorPassword = applicationFour.getInnovatorPassword();
    innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    corporateProfileId = passcodeApp.getCorporatesProfileId();
    consumerProfileId = passcodeApp.getConsumersProfileId();
    corporateManagedAccountProfileId = passcodeApp.getCorporatePayneticsEeaManagedAccountsProfileId();
    consumerManagedAccountProfileId = passcodeApp.getConsumerPayneticsEeaManagedAccountsProfileId();
    corporatePrepaidManagedCardProfileId = passcodeApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
    corporateDebitManagedCardProfileId = passcodeApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
    consumerPrepaidManagedCardProfileId = passcodeApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
    transfersProfileId = passcodeApp.getTransfersProfileId();
    sendsProfileId = passcodeApp.getSendProfileId();
    outgoingWireTransfersProfileId = passcodeApp.getOwtProfileId();
    secretKey = passcodeApp.getSecretKey();
    sharedKey = passcodeApp.getSharedKey();
    programmeId = passcodeApp.getProgrammeId();
    webhookServiceDetails = WebhookHelper.generateWebhookUrl();
    adminImpersonatedTenantToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());

    InnovatorHelper.enableWebhook(
            UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
            programmeId, innovatorToken);
  }

  @AfterAll
  public static void tearDown() {
    InnovatorHelper.disableWebhook(
            UpdateProgrammeModel.WebHookUrlSetup(scaMcApp.getProgrammeId(), true, webhookServiceDetails.getRight()),
            scaMcApp.getProgrammeId(), innovatorToken);
  }

  protected static String getBackofficeImpersonateToken(final String identityId,
                                                        final IdentityType identityType) {
    return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
  }
}
