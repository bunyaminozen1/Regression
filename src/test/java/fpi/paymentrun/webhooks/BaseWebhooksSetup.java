package fpi.paymentrun.webhooks;

import fpi.paymentrun.BaseSetupExtension;
import fpi.paymentrun.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@Tag(PluginsTags.PAYMENT_RUN_WEBHOOKS)
public class BaseWebhooksSetup {

    @RegisterExtension
    static fpi.paymentrun.BaseSetupExtension setupExtension = new BaseSetupExtension();
    protected final static String UBO_DIRECTOR_QUESTIONNAIRE_ID = "ubo_director_questionnaire";

    protected static ProgrammeDetailsModel pluginsWebhooksApp;

    protected static String programmeId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String zeroBalanceManagedAccountProfileId;
    protected static String linkedManagedAccountProfileId;
    protected static String prepaidCardProfileId;
    protected static String debitCardProfileId;
    protected static String innovatorToken;
    protected static String innovatorId;
    protected static String adminToken;
    protected static String impersonatedAdminToken;

    protected final static Pair<String, String> webhookServiceDetails = WebhookHelper.generateWebhookUrl();

    @BeforeAll
    public static void GlobalSetup() {
        pluginsWebhooksApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_WEBHOOKS_APP);

        programmeId = pluginsWebhooksApp.getProgrammeId();
        secretKey = pluginsWebhooksApp.getSecretKey();
        sharedKey = pluginsWebhooksApp.getSharedKey();
        corporateProfileId = pluginsWebhooksApp.getCorporatesProfileId();
        consumerProfileId = pluginsWebhooksApp.getConsumersProfileId();
        zeroBalanceManagedAccountProfileId = pluginsWebhooksApp.getZeroBalanceManagedAccountsProfileId();
        linkedManagedAccountProfileId = pluginsWebhooksApp.getLinkedAccountsProfileId();
        prepaidCardProfileId = pluginsWebhooksApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileId = pluginsWebhooksApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        innovatorToken = InnovatorHelper.loginInnovator(pluginsWebhooksApp.getInnovatorEmail(),pluginsWebhooksApp.getInnovatorPassword());
        innovatorId = pluginsWebhooksApp.getInnovatorId();
        adminToken = AdminService.loginAdmin();
        impersonatedAdminToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());

        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
                programmeId, InnovatorHelper.loginInnovator(pluginsWebhooksApp.getInnovatorEmail(), pluginsWebhooksApp.getInnovatorPassword()));
    }
}
