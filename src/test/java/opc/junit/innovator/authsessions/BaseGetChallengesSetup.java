package opc.junit.innovator.authsessions;

import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

@Execution(ExecutionMode.CONCURRENT)
public class BaseGetChallengesSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel scaSendsApp;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String sendsProfileId;
    protected static String outgoingWireTransfersProfileId;
    protected static String secretKey;
    protected static String programmeId;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;

    protected static String nonFpsInnovatorEmail;
    protected static String nonFpsInnovatorPassword;

    @BeforeAll
    public static void GlobalSetup() throws InterruptedException {

        scaSendsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_SENDS_APP);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        programmeId = scaSendsApp.getProgrammeId();

        innovatorId = scaSendsApp.getInnovatorId();
        innovatorEmail = scaSendsApp.getInnovatorEmail();
        innovatorPassword = scaSendsApp.getInnovatorPassword();

        corporateProfileId = scaSendsApp.getCorporatesProfileId();
        consumerProfileId = scaSendsApp.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = scaSendsApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = scaSendsApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateManagedAccountProfileId = scaSendsApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = scaSendsApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        sendsProfileId = scaSendsApp.getSendProfileId();
        outgoingWireTransfersProfileId = scaSendsApp.getOwtProfileId();
        secretKey = scaSendsApp.getSecretKey();

        nonFpsInnovatorEmail = nonFpsTenant.getInnovatorEmail();
        nonFpsInnovatorPassword = nonFpsTenant.getInnovatorPassword();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        String adminToken = AdminService.loginAdmin();

        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeId, adminToken);

        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeId, resetCount, adminToken);
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken) {
        return createManagedAccount(managedAccountProfileId, currency, authenticationToken, secretKey);
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken,
                                                                                  final String secretKey) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }

    protected static void fundManagedAccount(final String managedAccountId,
                                             final String currency,
                                             final Long depositAmount) {
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    protected static Pair<String, CreateManagedCardModel> createPrepaidManagedCard(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String authenticationToken) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }
}
