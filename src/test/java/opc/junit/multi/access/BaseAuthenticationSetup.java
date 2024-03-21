package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.AUTHENTICATION)
public class BaseAuthenticationSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel applicationThree;
    protected static ProgrammeDetailsModel applicationFour;
    protected static ProgrammeDetailsModel passcodeApp;
    protected static ProgrammeDetailsModel semiScaPasscodeApp;
    protected static ProgrammeDetailsModel scaApp;
    protected static ProgrammeDetailsModel scaMaApp;
    protected static ProgrammeDetailsModel scaMcApp;
    protected static ProgrammeDetailsModel nonFpsEnabledTenant;
    protected static ProgrammeDetailsModel secondaryScaApp;
    protected static String programmeId;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String semiInnovatorToken;
    protected static String passcodeAppCorporateProfileId;
    protected static String passcodeAppConsumerProfileId;
    protected static String passcodeAppConsumerManagedAccountProfileId;
    protected static String passcodeAppCorporateManagedAccountProfileId;
    protected static String passcodeAppConsumerManagedCardProfileId;
    protected static String passcodeAppCorporateManagedCardProfileId;
    protected static String passcodeAppSecretKey;
    protected static String passcodeAppSharedKey;
    protected static String adminToken;

    protected static Pair<String, String> webhookServiceDetails;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
        applicationFour = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_FOUR);
        scaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_APP);
        passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);
        semiScaPasscodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SEMI_SCA_PASSCODE_APP);
        scaMaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_MA_APP);
        scaMcApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_MC_APP);
        nonFpsEnabledTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        secondaryScaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SECONDARY_SCA_APP);

        programmeId = applicationOne.getProgrammeId();
        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        semiInnovatorToken = InnovatorHelper.loginInnovator(semiScaPasscodeApp.getInnovatorEmail(), semiScaPasscodeApp.getInnovatorPassword());
        passcodeAppCorporateProfileId = passcodeApp.getCorporatesProfileId();
        passcodeAppConsumerProfileId = passcodeApp.getConsumersProfileId();
        passcodeAppConsumerManagedAccountProfileId = passcodeApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        passcodeAppCorporateManagedAccountProfileId = passcodeApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        passcodeAppConsumerManagedCardProfileId = passcodeApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppCorporateManagedCardProfileId = passcodeApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppSecretKey = passcodeApp.getSecretKey();
        passcodeAppSharedKey = passcodeApp.getSharedKey();
        adminToken = AdminService.loginAdmin();

        webhookServiceDetails = WebhookHelper.generateWebhookUrl();

        updateWebhookSettings(true, List.of(passcodeApp, scaMaApp, scaMcApp), innovatorToken);
        updateWebhookSettings(true, List.of(semiScaPasscodeApp), semiInnovatorToken);
    }

    @AfterAll
    public static void disableWebhooks() {

        updateWebhookSettings(false, List.of(scaMaApp, scaMcApp), innovatorToken);
        updateWebhookSettings(false, List.of(semiScaPasscodeApp), semiInnovatorToken);
    }

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }

    private static void updateWebhookSettings(final boolean isEnabled,
                                              final List<ProgrammeDetailsModel> programmes,
                                              final String token) {

        if (isEnabled) {
            programmes.forEach(programme ->
                    InnovatorHelper.enableWebhook(
                            UpdateProgrammeModel.WebHookUrlSetup(programme.getProgrammeId(), false, webhookServiceDetails.getRight()),
                            programme.getProgrammeId(), token));
        } else {
            programmes.forEach(programme ->
                    InnovatorHelper.disableWebhook(
                            UpdateProgrammeModel.WebHookUrlSetup(programme.getProgrammeId(), true, webhookServiceDetails.getRight()),
                            programme.getProgrammeId(), token));
        }
    }
}
