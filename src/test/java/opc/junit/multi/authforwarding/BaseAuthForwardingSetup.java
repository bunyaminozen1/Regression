package opc.junit.multi.authforwarding;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.innovator.AbstractCreateManagedCardsProfileModel;
import opc.models.innovator.CreateDebitManagedCardsProfileModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.CreatePrepaidManagedCardsProfileModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.managedcards.AuthForwardingModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.services.innovatornew.InnovatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Random;

import static org.apache.http.HttpStatus.SC_OK;


@Execution(ExecutionMode.SAME_THREAD)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.AUTH_FORWARDING)
public class BaseAuthForwardingSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardProfileId;
    protected static String consumerPrepaidManagedCardProfileId;
    protected static String corporateDebitManagedCardProfileId;
    protected static String consumerDebitManagedCardProfileId;
    protected static String transfersProfileId;
    protected static String sendsProfileId;
    protected static String outgoingWireTransfersProfileId;
    protected static String secretKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static Pair<String, String> webhookServiceDetails;
    protected static Pair<String, String> authForwardingWebhookServiceDetails;
    protected static String adminImpersonatedTenantToken;
    protected static String programmeId;

    @BeforeAll
    public static void GlobalSetup(){

        final ProgrammeDetailsModel webhooksApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.WEBHOOKS_APP);

        innovatorId = webhooksApp.getInnovatorId();
        innovatorEmail = webhooksApp.getInnovatorEmail();
        innovatorPassword = webhooksApp.getInnovatorPassword();

        corporateProfileId = webhooksApp.getCorporatesProfileId();
        consumerProfileId = webhooksApp.getConsumersProfileId();

        corporateManagedAccountProfileId = webhooksApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = webhooksApp.getConsumerPayneticsEeaManagedAccountsProfileId();

        transfersProfileId = webhooksApp.getTransfersProfileId();
        sendsProfileId = webhooksApp.getSendProfileId();
        outgoingWireTransfersProfileId = webhooksApp.getOwtProfileId();
        secretKey = webhooksApp.getSecretKey();

        programmeId = webhooksApp.getProgrammeId();

        final String innovatorEmail = webhooksApp.getInnovatorEmail();
        final String innovatorPassword = webhooksApp.getInnovatorPassword();
        final String programmeId = webhooksApp.getProgrammeId();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        webhookServiceDetails = WebhookHelper.generateWebhookUrl();

        adminImpersonatedTenantToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());

        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
                programmeId, innovatorToken);
    }
    private static CreateManagedCardsProfileV2Model getCorporateDebitAuthForwardingModel(final boolean authForwardingEnabled, final String defaultTimeoutDecision){
        return CreateManagedCardsProfileV2Model.builder()
                .createDebitProfileRequest(CreateDebitManagedCardsProfileModel
                        .DefaultCorporateCreateDebitManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultCorporateDebitCreateManagedCardsProfileModel()
                                .authForwarding(new AuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision))
                                .build())
                        .build())
                .cardFundingType("DEBIT")
                .build();
    }

    private static CreateManagedCardsProfileV2Model getCorporatePrepaidAuthForwardingModel(final boolean authForwardingEnabled, final String defaultTimeoutDecision){
        return CreateManagedCardsProfileV2Model.builder()
                .createPrepaidProfileRequest(CreatePrepaidManagedCardsProfileModel
                        .DefaultCorporateCreatePrepaidManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultCorporatePrepaidCreateManagedCardsProfileModel()
                                .authForwarding(new AuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision))
                                .build())
                        .build())
                .cardFundingType("PREPAID")
                .build();
    }

    private static CreateManagedCardsProfileV2Model getConsumerDebitAuthForwardingModel(final boolean authForwardingEnabled, final String defaultTimeoutDecision){
        return CreateManagedCardsProfileV2Model.builder()
                .createDebitProfileRequest(CreateDebitManagedCardsProfileModel
                        .DefaultConsumerCreateDebitManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultConsumerDebitCreateManagedCardsProfileModel()
                                .authForwarding(new AuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision))
                                .build())
                        .build())
                .cardFundingType("DEBIT")
                .build();
    }

    private static CreateManagedCardsProfileV2Model getConsumerPrepaidAuthForwardingModel(final boolean authForwardingEnabled, final String defaultTimeoutDecision){
        return CreateManagedCardsProfileV2Model.builder()
                .createPrepaidProfileRequest(CreatePrepaidManagedCardsProfileModel
                        .DefaultConsumerCreatePrepaidManagedCardsProfileModel()
                        .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                                .DefaultConsumerPrepaidCreateManagedCardsProfileModel()
                                .authForwarding(new AuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision))
                                .build())
                        .build())
                .cardFundingType("PREPAID")
                .build();
    }

    protected static String getCorporateDebitManagedCardProfileId(final boolean authForwardingEnabled, final String defaultTimeoutDecision) {
        return InnovatorService
                .createManagedCardsProfileV2(
                        getCorporateDebitAuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision),
                        innovatorToken,
                        programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");
    }

    protected static String getCorporatePrepaidManagedCardProfileId(final boolean authForwardingEnabled, final String defaultTimeoutDecision) {
        return InnovatorService
                .createManagedCardsProfileV2(
                        getCorporatePrepaidAuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision),
                        innovatorToken,
                        programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");
    }

    protected static String getConsumerDebitManagedCardProfileId(final boolean authForwardingEnabled, final String defaultTimeoutDecision) {
        return InnovatorService
                .createManagedCardsProfileV2(
                        getConsumerDebitAuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision),
                        innovatorToken,
                        programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");
    }

    protected static String getConsumerPrepaidManagedCardProfileId(final boolean authForwardingEnabled, final String defaultTimeoutDecision) {
        return InnovatorService
                .createManagedCardsProfileV2(
                        getConsumerPrepaidAuthForwardingModel(authForwardingEnabled, defaultTimeoutDecision),
                        innovatorToken,
                        programmeId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");
    }

    protected static Long getRandomPurchaseAmount() {
        Random random = new Random();
        return (long) (random.nextInt(10 - 5) + 5) * 10;
    }

    protected static Long getRandomDepositAmount() {
        Random random = new Random();
        return (long) (random.nextInt(50 - 20) + 20) * 10;
    }
}
