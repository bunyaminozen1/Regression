package opc.junit.multi.sca;


import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.admin.SetScaModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
public class BaseScaSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel scaApp;
    protected static ProgrammeDetailsModel scaMaApp;
    protected static ProgrammeDetailsModel scaMcApp;
    protected static ProgrammeDetailsModel scaEnrolApp;
    protected static ProgrammeDetailsModel secondaryScaApp;

    protected static String corporateProfileIdScaApp;
    protected static String consumerProfileIdScaApp;
    protected static String secretKeyScaApp;
    protected static String secretKeyScaEnrolApp;
    protected static String corporateManagedAccountProfileIdScaApp;
    protected static String consumerManagedAccountProfileIdScaApp;
    protected static String corporatePrepaidManagedCardProfileIdScaApp;
    protected static String consumerPrepaidManagedCardProfileIdScaApp;
    protected static String programmeIdScaApp;
    protected static String innovatorEmailScaApp;
    protected static String innovatorIdScaApp;
    protected static String transfersProfileIdScaApp;
    protected static String sendsProfileIdScaApp;
    protected static String outgoingWireTransfersProfileIdScaApp;

    protected static String corporateProfileIdScaMaApp;
    protected static String consumerProfileIdScaMaApp;
    protected static String secretKeyScaMaApp;
    protected static String corporateManagedAccountProfileIdScaMaApp;
    protected static String consumerManagedAccountProfileIdScaMaApp;
    protected static String corporatePrepaidManagedCardProfileIdScaMaApp;
    protected static String consumerPrepaidManagedCardProfileIdScaMaApp;
    protected static String programmeIdScaMaApp;
    protected static String innovatorEmailScaMaApp;
    protected static String innovatorPasswordScaMaApp;
    protected static String innovatorIdScaMaApp;
    protected static String transfersProfileIdScaMaApp;
    protected static String sendsProfileIdScaMaApp;
    protected static String outgoingWireTransfersProfileIdScaMaApp;

    protected static String corporateProfileIdScaMcApp;
    protected static String corporateProfileIdScaEnrolApp;
    protected static String consumerProfileIdScaMcApp;
    protected static String consumerProfileIdScaEnrolApp;
    protected static String programmeIdScaEnrolApp;

    protected static String secretKeyScaMcApp;
    protected static String corporateManagedAccountProfileIdScaMcApp;
    protected static String consumerManagedAccountProfileIdScaMcApp;
    protected static String corporatePrepaidManagedCardProfileIdScaMcApp;
    protected static String consumerPrepaidManagedCardProfileIdScaMcApp;
    protected static String programmeIdScaMcApp;
    protected static String innovatorEmailScaMcApp;
    protected static String innovatorPasswordScaMcApp;
    protected static String innovatorIdScaMcApp;
    protected static String transfersProfileIdScaMcApp;
    protected static String sendsProfileIdScaMcApp;
    protected static String outgoingWireTransfersProfileIdScaMcApp;

    protected static String adminToken;

    protected static String innovatorTokenScaMcApp;
    protected static String innovatorTokenScaMaApp;
    protected static String secretKeySecondaryScaApp;
    protected static String sharedKeySecondaryScaApp;

    @BeforeAll
    public static void GlobalSetup() {
        scaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_APP);
        scaMaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_MA_APP);
        scaMcApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_MC_APP);
        scaEnrolApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_ENROL_APP);
        secondaryScaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SECONDARY_SCA_APP);


        corporateProfileIdScaApp = scaApp.getCorporatesProfileId();
        consumerProfileIdScaApp = scaApp.getConsumersProfileId();
        corporateManagedAccountProfileIdScaApp = scaApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileIdScaApp = scaApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardProfileIdScaApp = scaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardProfileIdScaApp = scaApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        programmeIdScaApp = scaApp.getProgrammeId();
        innovatorEmailScaApp = scaApp.getInnovatorEmail();
        innovatorIdScaApp = scaApp.getInnovatorId();
        transfersProfileIdScaApp = scaApp.getTransfersProfileId();
        sendsProfileIdScaApp = scaApp.getSendProfileId();
        outgoingWireTransfersProfileIdScaApp = scaApp.getOwtProfileId();

        corporateProfileIdScaMaApp = scaMaApp.getCorporatesProfileId();
        consumerProfileIdScaMaApp = scaMaApp.getConsumersProfileId();
        corporateManagedAccountProfileIdScaMaApp = scaMaApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileIdScaMaApp = scaMaApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardProfileIdScaMaApp = scaMaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardProfileIdScaMaApp = scaMaApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        programmeIdScaMaApp = scaMaApp.getProgrammeId();
        innovatorEmailScaMaApp = scaMaApp.getInnovatorEmail();
        innovatorPasswordScaMaApp = scaMaApp.getInnovatorPassword();
        innovatorIdScaMaApp = scaMaApp.getInnovatorId();
        transfersProfileIdScaMaApp = scaMaApp.getTransfersProfileId();
        sendsProfileIdScaMaApp = scaMaApp.getSendProfileId();
        outgoingWireTransfersProfileIdScaMaApp = scaMaApp.getOwtProfileId();

        corporateProfileIdScaMcApp = scaMcApp.getCorporatesProfileId();
        consumerProfileIdScaMcApp = scaMcApp.getConsumersProfileId();
        corporateManagedAccountProfileIdScaMcApp = scaMcApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileIdScaMcApp = scaMcApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardProfileIdScaMcApp = scaMcApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardProfileIdScaMcApp = scaMcApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        programmeIdScaMcApp = scaMcApp.getProgrammeId();
        innovatorEmailScaMcApp = scaMcApp.getInnovatorEmail();
        innovatorPasswordScaMcApp = scaMcApp.getInnovatorPassword();
        innovatorIdScaMcApp = scaMcApp.getInnovatorId();
        transfersProfileIdScaMcApp = scaMcApp.getTransfersProfileId();
        sendsProfileIdScaMcApp = scaMcApp.getSendProfileId();
        outgoingWireTransfersProfileIdScaMcApp = scaMcApp.getOwtProfileId();

        secretKeyScaApp = scaApp.getSecretKey();
        secretKeyScaMaApp = scaMaApp.getSecretKey();
        secretKeyScaMcApp = scaMcApp.getSecretKey();
        secretKeyScaEnrolApp=scaEnrolApp.getSecretKey();

        corporateProfileIdScaEnrolApp = scaEnrolApp.getCorporatesProfileId();
        consumerProfileIdScaEnrolApp = scaEnrolApp.getConsumersProfileId();
        programmeIdScaEnrolApp = scaEnrolApp.getProgrammeId();

        adminToken = AdminService.loginAdmin();

        innovatorTokenScaMcApp = InnovatorHelper.loginInnovator(innovatorEmailScaMcApp, innovatorPasswordScaMcApp);
        innovatorTokenScaMaApp = InnovatorHelper.loginInnovator(innovatorEmailScaMaApp, innovatorPasswordScaMaApp);

        secretKeySecondaryScaApp = secondaryScaApp.getSecretKey();
        sharedKeySecondaryScaApp = secondaryScaApp.getSharedKey();

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.resetProgrammeAuthyLimitsCounter(scaApp.getProgrammeId(), adminToken);
        AdminHelper.resetProgrammeAuthyLimitsCounter(scaMaApp.getProgrammeId(), adminToken);
        AdminHelper.resetProgrammeAuthyLimitsCounter(scaMcApp.getProgrammeId(), adminToken);
        AdminHelper.resetProgrammeAuthyLimitsCounter(scaEnrolApp.getProgrammeId(), adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaApp.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaMaApp.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaMcApp.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaEnrolApp.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(secondaryScaApp.getProgrammeId(), resetCount, adminToken);

        AdminHelper.setSca(adminToken, programmeIdScaMaApp, true, false);
        AdminHelper.setSca(adminToken, programmeIdScaMcApp, false, true);

        final SetScaModel scaModel = SetScaModel.builder().scaAuthUserEnabled(true).build();
        AdminService.setScaConfig(adminToken, secondaryScaApp.getProgrammeId(), scaModel)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @AfterAll
    public static void disableSca(){
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaApp.getProgrammeId(), resetCount, adminToken);
    }
}
