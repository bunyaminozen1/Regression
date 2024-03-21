package opc.junit.semi;

import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
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
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

import static opc.junit.helpers.innovator.InnovatorHelper.enableAuthy;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.SEMI)
public class BaseSemiSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static ProgrammeDetailsModel semiPasscodeApp;
    protected static ProgrammeDetailsModel semiScaSendsApp;
    protected static ProgrammeDetailsModel scaEnrolApp;
    protected static ProgrammeDetailsModel threeDSApp;

    protected static String consumersProfileId;
    protected static String corporatesProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String tenantId;
    protected static String corporateManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String transfersProfileId;
    protected static String outgoingWireTransfersProfileId;
    protected static String sendsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String programmeId;

    protected static String secretKeyAppTwo;
    protected static String corporatesProfileIdAppTwo;

    protected static String corporatesProfileIdAppOne;
    protected static String secretKeyAppOne;

    protected static String passcodeAppProgrammeId;
    protected static String passcodeAppCorporateProfileId;
    protected static String passcodeAppConsumerProfileId;
    protected static String passcodeAppCorporateManagedAccountProfileId;
    protected static String passcodeAppConsumerManagedAccountProfileId;
    protected static String passcodeAppCorporatePrepaidManagedCardsProfileId;
    protected static String passcodeAppConsumerPrepaidManagedCardsProfileId;
    protected static String passcodeAppSendsProfileId;
    protected static String passcodeAppOutgoingWireTransfersProfileId;
    protected static String passcodeAppSecretKey;
    protected static String passcodeAppSharedKey;
    protected static String passcodeAppTenantId;
    protected static String passcodeAppInnovatorEmail;
    protected static String passcodeAppInnovatorPassword;

    protected static String corporateProfileIdScaSendsApp;
    protected static String consumerProfileIdScaSendsApp;
    protected static String corporatePrepaidManagedCardsProfileIdScaSendsApp;
    protected static String consumerPrepaidManagedCardsProfileIdScaSendsApp;
    protected static String corporateDebitManagedCardsProfileIdScaSendsApp;
    protected static String consumerDebitManagedCardsProfileIdScaSendsApp;
    protected static String corporateManagedAccountProfileIdScaSendsApp;
    protected static String consumerManagedAccountProfileIdScaSendsApp;
    protected static String secretKeyScaSendsApp;
    protected static String sendsProfileIdScaSendsApp;
    protected static String programmeIdScaSendsApp;
    protected static String programmeNameScaSendsApp;
    protected static String innovatorEmailScaSendApp;
    protected static String innovatorPasswordScaSendApp;
    protected static String tenantScaSendApp;

    protected static String programmeIdThreeDSApp;
    protected static String secretKeyThreeDSApp;
    protected static String sharedKeyThreeDSApp;
    protected static String tenantIdThreeDSApp;
    protected static String corporateProfileIdThreeDSApp;
    protected static String consumerProfileIdThreeDSApp;
    protected static String corporateManagedAccountsProfileIdThreeDSApp;
    protected static String corporatePrepaidManagedCardsProfileIdThreeDSApp;
    protected static String corporateDebitManagedCardsProfileIdThreeDSApp;
    protected static String transfersProfileIdThreeDSApp;


    protected static String adminToken;

    //TODO: to add cases for EnrolPushStepUp when semi innovator for ScaEnrolApp will be created.
    //protected static String corporateProfileIdScaEnrolApp;
    //protected static String consumerProfileIdScaEnrolApp;
    //protected static String programmeIdScaEnrolApp;
    //protected static String secretKeyScaEnrolApp;
    //
    //protected static String adminToken;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        semiPasscodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SEMI_PASSCODE_APP);
        semiScaSendsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SEMI_SCA_SENDS_APP);
        threeDSApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.THREE_DS_APP);
        //scaEnrolApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_ENROL_APP);


        consumersProfileId = nonFpsTenant.getConsumersProfileId();
        corporatesProfileId = nonFpsTenant.getCorporatesProfileId();
        secretKey = nonFpsTenant.getSecretKey();
        sharedKey = nonFpsTenant.getSharedKey();
        tenantId = nonFpsTenant.getInnovatorId();
        corporateManagedAccountProfileId = nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardsProfileId = nonFpsTenant.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        transfersProfileId = nonFpsTenant.getTransfersProfileId();
        outgoingWireTransfersProfileId = nonFpsTenant.getOwtProfileId();
        sendsProfileId = nonFpsTenant.getSendProfileId();
        corporateDebitManagedCardsProfileId = nonFpsTenant.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        innovatorEmail = nonFpsTenant.getInnovatorEmail();
        innovatorPassword = nonFpsTenant.getInnovatorPassword();
        programmeId = nonFpsTenant.getProgrammeId();
        secretKeyAppTwo = applicationTwo.getSecretKey();
        corporatesProfileIdAppTwo = applicationTwo.getCorporatesProfileId();

        passcodeAppProgrammeId = semiPasscodeApp.getProgrammeId();
        passcodeAppCorporateProfileId = semiPasscodeApp.getCorporatesProfileId();
        passcodeAppConsumerProfileId = semiPasscodeApp.getConsumersProfileId();
        passcodeAppCorporatePrepaidManagedCardsProfileId = semiPasscodeApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppConsumerPrepaidManagedCardsProfileId = semiPasscodeApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppCorporateManagedAccountProfileId = semiPasscodeApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        passcodeAppConsumerManagedAccountProfileId = semiPasscodeApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        passcodeAppSendsProfileId = semiPasscodeApp.getSendProfileId();
        passcodeAppOutgoingWireTransfersProfileId = semiPasscodeApp.getOwtProfileId();
        passcodeAppSecretKey = semiPasscodeApp.getSecretKey();
        passcodeAppSharedKey = semiPasscodeApp.getSharedKey();
        passcodeAppTenantId = semiPasscodeApp.getInnovatorId();
        passcodeAppInnovatorEmail = semiPasscodeApp.getInnovatorEmail();
        passcodeAppInnovatorPassword = semiPasscodeApp.getInnovatorPassword();


        corporateProfileIdScaSendsApp = semiScaSendsApp.getCorporatesProfileId();
        consumerProfileIdScaSendsApp = semiScaSendsApp.getConsumersProfileId();
        corporatePrepaidManagedCardsProfileIdScaSendsApp = semiScaSendsApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileIdScaSendsApp = semiScaSendsApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileIdScaSendsApp = semiScaSendsApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileIdScaSendsApp = semiScaSendsApp.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountProfileIdScaSendsApp = semiScaSendsApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileIdScaSendsApp = semiScaSendsApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        secretKeyScaSendsApp = semiScaSendsApp.getSecretKey();
        sendsProfileIdScaSendsApp = semiScaSendsApp.getSendProfileId();
        programmeIdScaSendsApp = semiScaSendsApp.getProgrammeId();
        programmeNameScaSendsApp = semiScaSendsApp.getProgrammeName();
        innovatorEmailScaSendApp = semiScaSendsApp.getInnovatorEmail();
        innovatorPasswordScaSendApp = semiScaSendsApp.getInnovatorPassword();
        tenantScaSendApp = semiScaSendsApp.getInnovatorId();

        corporatesProfileIdAppOne = applicationOne.getCorporatesProfileId();
        secretKeyAppOne = applicationOne.getSecretKey();

        programmeIdThreeDSApp = threeDSApp.getProgrammeId();
        secretKeyThreeDSApp = threeDSApp.getSecretKey();
        sharedKeyThreeDSApp = threeDSApp.getSharedKey();
        tenantIdThreeDSApp = threeDSApp.getInnovatorId();
        corporateProfileIdThreeDSApp = threeDSApp.getThreeDSCorporatesProfileId();
        consumerProfileIdThreeDSApp = threeDSApp.getThreeDSConsumersProfileId();
        corporatePrepaidManagedCardsProfileIdThreeDSApp = threeDSApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileIdThreeDSApp = threeDSApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountsProfileIdThreeDSApp = threeDSApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        transfersProfileIdThreeDSApp = threeDSApp.getTransfersProfileId();

        //TODO Enable as part of the initial data and remove this call
        //Enable Authy for nonFpsTenant
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        enableAuthy(programmeId, innovatorToken);
        adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeId, resetCount, adminToken);

        //corporateProfileIdScaEnrolApp = scaEnrolApp.getCorporatesProfileId();
        //consumerProfileIdScaEnrolApp = scaEnrolApp.getConsumersProfileId();
        //programmeIdScaEnrolApp = scaEnrolApp.getProgrammeId();
        //secretKeyScaEnrolApp=scaEnrolApp.getSecretKey();
        //
        //adminToken = AdminService.loginAdmin();

    }

}
