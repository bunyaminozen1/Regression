package opc.junit.smoke;

import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.InstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.admin.ConsumerWithKycResponseModel;
import opc.models.admin.ConsumersWithKycResponseModel;
import opc.models.admin.CorporateWithKybResponseModel;
import opc.models.admin.CorporatesWithKybResponseModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.SMOKE)
public class BaseSmokeSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String programmeId;
    protected static String programmeName;

    protected static String semiCorporatesProfileId;
    protected static String semiConsumersProfileId;
    protected static String semiCorporateManagedAccountProfileId;
    protected static String semiCorporateDebitManagedCardsProfileId;
    protected static String semiCorporatePrepaidManagedCardsProfileId;
    protected static String semiTransfersProfileId;
    protected static String semiOutgoingWireTransfersProfileId;
    protected static String semiSecretKey;
    protected static String semiSendsProfileId;
    protected static String semiSharedKey;
    protected static String semiInnovatorId;
    protected static String semiInnovatorEmail;
    protected static String semiInnovatorPassword;
    protected static String semiProgrammeId;
    protected static ProgrammeDetailsModel passcodeApp;
    protected static String passcodeAppCorporateProfileId;
    protected static String passcodeAppConsumerProfileId;
    protected static String passcodeAppSecretKey;
    protected static String passcodeAppSharedKey;

    protected static Pair<String, String> webhookServiceDetails;

    protected static String adminToken;
    protected static String adminImpersonatedTenantToken;

    protected static String transfersProfileId;
    protected static String sendsProfileId;
    protected static String outgoingWireTransfersProfileId;

    protected static ProgrammeDetailsModel threeDSApp;
    protected static String threeDSConsumerProfileId;
    protected static String threeDSAppCorporateProfileId;
    protected static String threeDSAppConsumerProfileId;
    protected static String threeDSCorporateProfileId;
    protected static String threeDSAppSecretKey;
    protected static String threeDSAppSharedKey;
    protected static String threeDSAppProgrammeId;
    protected static String threeDSAppCorporatePrepaidManagedCardsProfileId;
    protected static String threeDSAppConsumerPrepaidManagedCardsProfileId;
    protected static String threeDSAppCorporateDebitManagedCardsProfileId;
    protected static String threeDSAppConsumerDebitManagedCardsProfileId;
    protected static String threeDSAppCorporateManagedAccountsProfileId;
    protected static String threeDSAppConsumerManagedAccountsProfileId;
    protected static String threeDSAppInnovatorToken;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        corporateManagedAccountsProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();

        transfersProfileId = applicationOne.getTransfersProfileId();
        sendsProfileId = applicationOne.getSendProfileId();
        outgoingWireTransfersProfileId = applicationOne.getOwtProfileId();
        programmeId = applicationOne.getProgrammeId();
        programmeName = applicationOne.getProgrammeName();
        adminToken = AdminService.loginAdmin();
        adminImpersonatedTenantToken = AdminService.impersonateTenant(innovatorId, adminToken);
        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();

        semiConsumersProfileId = nonFpsTenant.getConsumersProfileId();
        semiCorporatesProfileId = nonFpsTenant.getCorporatesProfileId();
        semiSecretKey = nonFpsTenant.getSecretKey();
        semiSharedKey = nonFpsTenant.getSharedKey();
        semiInnovatorId = nonFpsTenant.getInnovatorId();
        semiCorporateManagedAccountProfileId = nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId();
        semiCorporatePrepaidManagedCardsProfileId = nonFpsTenant.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        semiTransfersProfileId = nonFpsTenant.getTransfersProfileId();
        semiOutgoingWireTransfersProfileId = nonFpsTenant.getOwtProfileId();
        semiSendsProfileId = nonFpsTenant.getSendProfileId();
        semiCorporateDebitManagedCardsProfileId = nonFpsTenant.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        semiInnovatorEmail = nonFpsTenant.getInnovatorEmail();
        semiInnovatorPassword = nonFpsTenant.getInnovatorPassword();
        semiProgrammeId = nonFpsTenant.getProgrammeId();

        passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);
        passcodeAppCorporateProfileId = passcodeApp.getCorporatesProfileId();
        passcodeAppConsumerProfileId = passcodeApp.getConsumersProfileId();
        passcodeAppSecretKey = passcodeApp.getSecretKey();
        passcodeAppSharedKey = passcodeApp.getSharedKey();
        webhookServiceDetails = WebhookHelper.generateWebhookUrl();

        InnovatorHelper.enableWebhook(
                UpdateProgrammeModel.WebHookUrlSetup(passcodeApp.getProgrammeId(), false, webhookServiceDetails.getRight()),
                passcodeApp.getProgrammeId(), innovatorToken);

        threeDSApp = (ProgrammeDetailsModel) setupExtension.store.get(
                InnovatorSetup.THREE_DS_APP);

        threeDSAppCorporateProfileId = threeDSApp.getCorporatesProfileId();
        threeDSAppConsumerProfileId = threeDSApp.getConsumersProfileId();
        threeDSCorporateProfileId = threeDSApp.getThreeDSCorporatesProfileId();
        threeDSConsumerProfileId = threeDSApp.getThreeDSConsumersProfileId();
        threeDSAppSecretKey = threeDSApp.getSecretKey();
        threeDSAppSharedKey = threeDSApp.getSharedKey();
        threeDSAppProgrammeId = threeDSApp.getProgrammeId();

        threeDSAppCorporatePrepaidManagedCardsProfileId = threeDSApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        threeDSAppConsumerPrepaidManagedCardsProfileId = threeDSApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        threeDSAppCorporateDebitManagedCardsProfileId = threeDSApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        threeDSAppConsumerDebitManagedCardsProfileId = threeDSApp.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        threeDSAppCorporateManagedAccountsProfileId = threeDSApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        threeDSAppConsumerManagedAccountsProfileId = threeDSApp.getConsumerPayneticsEeaManagedAccountsProfileId();

        threeDSAppInnovatorToken = InnovatorHelper.loginInnovator(threeDSApp.getInnovatorEmail(),
                threeDSApp.getInnovatorPassword());

    }

    protected static Pair<String, String> getExistingConsumerDetails() {
        final ConsumersWithKycResponseModel consumers =
                AdminHelper.getConsumers(programmeId, adminToken);

        final ConsumerWithKycResponseModel consumer = consumers.getConsumerWithKyc()
                .stream().filter(x -> x.getConsumer().getProfileId().equals(consumerProfileId)).
                collect(Collectors.toList()).get(0);

        String existingConsumerToken = AuthenticationHelper.login(consumer.getConsumer().getRootUser().getEmail(),
                TestHelper.getDefaultPassword(secretKey), secretKey);
        String baseCurrency = consumer.getConsumer().getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumer.getConsumer().getId().getId());

        return Pair.of(existingConsumerToken, baseCurrency);
    }

    protected static Pair<String, String> getExistingCorporateDetails() {
        final CorporatesWithKybResponseModel corporates =
                AdminHelper.getCorporates(corporateProfileId, adminToken);

        final CorporateWithKybResponseModel corporate = corporates.getCorporateWithKyb().get(0);

        final String corporateEmail = corporate.getCorporate().getRootUser().getEmail();
        final String existingCorporateToken = AuthenticationHelper.login(corporateEmail,
                TestHelper.getDefaultPassword(secretKey), secretKey);
        final String baseCurrency = corporate.getCorporate().getBaseCurrency();

        return Pair.of(existingCorporateToken, baseCurrency);
    }

    protected static CorporateWithKybResponseModel getExistingCorporateWithBeneficiary(final String profileId) {
        final CorporatesWithKybResponseModel corporates =
                AdminHelper.getCorporatesSemi(profileId, adminToken, 30);

        return corporates
                .getCorporateWithKyb()
                .stream().filter(x -> !x.getCorporate().getRootUser().getBeneficiaryId().equals("0"))
                .collect(Collectors.toList()).get(0);
    }

    protected static Pair<String, String> getExistingConsumerDetailsPasscodeApp() {
        final ConsumersWithKycResponseModel consumers =
                AdminHelper.getConsumers(passcodeApp.getProgrammeId(), adminToken);

        final ConsumerWithKycResponseModel consumer = consumers.getConsumerWithKyc()
                .stream().filter(x -> x.getConsumer().getProfileId().equals(passcodeAppConsumerProfileId)).
                collect(Collectors.toList()).get(0);

        String existingConsumerToken = AuthenticationHelper.login(consumer.getConsumer().getRootUser().getEmail(),
                TestHelper.getDefaultPassword(secretKey), passcodeAppSecretKey);
        String existingConsumerId = consumer.getConsumer().getId().getId();

        return Pair.of(existingConsumerToken, existingConsumerId);
    }

    protected static Pair<String, String> getExistingCorporateDetailsPasscodeApp() {
        final CorporatesWithKybResponseModel corporates =
                AdminHelper.getCorporates(passcodeAppCorporateProfileId, adminToken);

        final CorporateWithKybResponseModel corporate = corporates.getCorporateWithKyb().get(0);

        final String corporateEmail = corporate.getCorporate().getRootUser().getEmail();
        final String existingCorporateToken = AuthenticationHelper.login(corporateEmail,
                TestHelper.getDefaultPassword(secretKey), passcodeAppSecretKey);
        final String existingCorporateId = corporate.getCorporate().getId().getId();

        return Pair.of(existingCorporateToken, existingCorporateId);
    }

    protected static List<InstrumentType> getInstrumentTypes() {
        return Arrays.asList(VIRTUAL, PHYSICAL);
    }

}
