package opc.junit.multi.webhooks;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.helpers.LimitsModelHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class BaseWebhooksSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();
    protected final static String UBO_DIRECTOR_QUESTIONNAIRE_ID = "ubo_director_questionnaire";

    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static ProgrammeDetailsModel passcodeApp;
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
    protected static String adminImpersonatedTenantToken;
    protected static String programmeId;
    protected static String adminToken;

    protected static String passcodeAppCorporateProfileId;
    protected static String passcodeAppConsumerProfileId;
    protected static String passcodeAppSecretKey;
    protected static String passcodeAppSharedKey;
    protected static String passcodeAppProgrammeId;
    protected static String passcodeAppInnovatorEmail;
    protected static String passcodeAppInnovatorPassword;
    protected static String passcodeAppInnovatorToken;
    protected static String passcodeAppCorporateManagedAccountProfileId;
    protected static String passcodeAppOutgoingWireTransfersProfileId;

    protected static String nonFpsInnovatorToken;
    protected static String nonFpsProgrammeId;

    protected final static Pair<String, String> webhookServiceDetails = WebhookHelper.generateWebhookUrl();

    final static int corporateMaxSum = 3000000;
    final static int consumerMaxSum = 500000;

    @BeforeAll
    public static void GlobalSetup(){

        final ProgrammeDetailsModel webhooksApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.WEBHOOKS_APP);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(
                InnovatorSetup.PASSCODE_APP);

        innovatorId = webhooksApp.getInnovatorId();
        innovatorEmail = webhooksApp.getInnovatorEmail();
        innovatorPassword = webhooksApp.getInnovatorPassword();

        corporateProfileId = webhooksApp.getCorporatesProfileId();
        consumerProfileId = webhooksApp.getConsumersProfileId();

        corporateManagedAccountProfileId = webhooksApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = webhooksApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardProfileId = webhooksApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardProfileId = webhooksApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerPrepaidManagedCardProfileId = webhooksApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();

        transfersProfileId = webhooksApp.getTransfersProfileId();
        sendsProfileId = webhooksApp.getSendProfileId();
        outgoingWireTransfersProfileId = webhooksApp.getOwtProfileId();
        secretKey = webhooksApp.getSecretKey();
        sharedKey = webhooksApp.getSharedKey();

        programmeId = webhooksApp.getProgrammeId();
        adminToken = AdminService.loginAdmin();
        adminImpersonatedTenantToken = AdminService.impersonateTenant(innovatorId, adminToken);

        passcodeAppCorporateProfileId = passcodeApp.getCorporatesProfileId();
        passcodeAppConsumerProfileId = passcodeApp.getConsumersProfileId();
        passcodeAppSecretKey = passcodeApp.getSecretKey();
        passcodeAppSharedKey = passcodeApp.getSharedKey();
        passcodeAppProgrammeId = passcodeApp.getProgrammeId();
        passcodeAppInnovatorEmail = passcodeApp.getInnovatorEmail();
        passcodeAppInnovatorPassword = passcodeApp.getInnovatorPassword();
        passcodeAppCorporateManagedAccountProfileId = passcodeApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        passcodeAppOutgoingWireTransfersProfileId = passcodeApp.getOwtProfileId();

        passcodeAppInnovatorToken = InnovatorHelper.loginInnovator(passcodeAppInnovatorEmail, passcodeAppInnovatorPassword);
        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(passcodeAppProgrammeId, false, webhookServiceDetails.getRight()),
                passcodeAppProgrammeId, passcodeAppInnovatorToken);

        nonFpsInnovatorToken = InnovatorHelper.loginInnovator(nonFpsTenant.getInnovatorEmail(), nonFpsTenant.getInnovatorPassword());
        nonFpsProgrammeId = nonFpsTenant.getProgrammeId();
        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(nonFpsProgrammeId, false, webhookServiceDetails.getRight()),
                nonFpsProgrammeId, nonFpsInnovatorToken);

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
                programmeId, InnovatorHelper.loginInnovator(webhooksApp.getInnovatorEmail(), webhooksApp.getInnovatorPassword()));

        opc.services.adminnew.AdminService.setSepaInstantLimit(
            LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CORPORATE, corporateMaxSum),
            adminToken
        );

        opc.services.adminnew.AdminService.setSepaInstantLimit(
            LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CONSUMER, consumerMaxSum),
            adminToken
        );


        opc.services.adminnew.AdminService.setSepaInstantIncomingWireTransferLimit(
            LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CORPORATE, corporateMaxSum),
            adminToken
        );

        opc.services.adminnew.AdminService.setSepaInstantIncomingWireTransferLimit(
            LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CONSUMER, consumerMaxSum),
            adminToken
        );
    }
}
