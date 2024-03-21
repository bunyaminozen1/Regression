package opc.junit.multi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mysql.cj.jdbc.ConnectionImpl;
import commons.config.ConfigHelper;
import opc.database.BaseDatabaseExtension;
import opc.database.DatabaseConnection;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.database.ProgrammeDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class BaseSetupExtension extends BaseDatabaseExtension implements BeforeAllCallback, TestExecutionListener {

    private static boolean INITIALISED = false;

    public ExtensionContext.Store store;

    private static List<ProgrammeDetailsModel> applications;
    private static List<ProgrammeDetailsModel> nonFpsApplications;
    private static List<ProgrammeDetailsModel> semiApplications;
    private static List<ProgrammeDetailsModel> ukApplications;
    private static ProgrammeDetailsModel applicationOne;
    private static ProgrammeDetailsModel applicationTwo;
    private static ProgrammeDetailsModel applicationThree;
    private static ProgrammeDetailsModel applicationFour;
    private static ProgrammeDetailsModel oddApp;
    private static ProgrammeDetailsModel scaApp;
    private static ProgrammeDetailsModel webhooksApp;
    private static ProgrammeDetailsModel nonFpsEnabledTenantDetails;
    private static ProgrammeDetailsModel scaMaApp;
    private static ProgrammeDetailsModel scaMcApp;
    private static ProgrammeDetailsModel webhooksOddApp;
    private static ProgrammeDetailsModel scaEnrolApp;
    private static ProgrammeDetailsModel threeDsApp;
    private static ProgrammeDetailsModel scaSendsApp;
    private static ProgrammeDetailsModel lowValueExemptionApp;
    private static ProgrammeDetailsModel passcodeApp;
    private static ProgrammeDetailsModel semiPasscodeApp;
    private static ProgrammeDetailsModel semiScaSendsApp;
    private static ProgrammeDetailsModel semiScaPasscodeApp;
    private static ProgrammeDetailsModel secondaryScaApp;
    private static ProgrammeDetailsModel applicationOneUk;
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {

        final String uniqueKey = this.getClass().getName();

        setUpInnovator();

        if (!INITIALISED) {
            INITIALISED = true;
            DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            context.getRoot().getStore(GLOBAL).put(uniqueKey, this);
            updateSecurityModel(applications);
            updateSecurityModel(nonFpsApplications);
            updateSecurityModel(semiApplications);
            updateSecurityModel(ukApplications);
        }

        if (DB_CONNECTION != null) {
            if (((ConnectionImpl) DB_CONNECTION).getSession().isClosed()) {
                DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            }
        }

        context.getStore(GLOBAL).put(InnovatorSetup.APPLICATION_ONE, applicationOne);
        context.getStore(GLOBAL).put(InnovatorSetup.APPLICATION_TWO, applicationTwo);
        context.getStore(GLOBAL).put(InnovatorSetup.APPLICATION_THREE, applicationThree);
        context.getStore(GLOBAL).put(InnovatorSetup.APPLICATION_FOUR, applicationFour);
        context.getStore(GLOBAL).put(InnovatorSetup.ODD_APP, oddApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SCA_APP, scaApp);
        context.getStore(GLOBAL).put(InnovatorSetup.WEBHOOKS_APP, webhooksApp);
        context.getStore(GLOBAL).put(InnovatorSetup.NON_FPS_ENABLED_TENANT, nonFpsEnabledTenantDetails);
        context.getStore(GLOBAL).put(InnovatorSetup.SCA_MA_APP, scaMaApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SCA_MC_APP, scaMcApp);
        context.getStore(GLOBAL).put(InnovatorSetup.WEBHOOKS_ODD_APP, webhooksOddApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SCA_ENROL_APP, scaEnrolApp);
        context.getStore(GLOBAL).put(InnovatorSetup.THREE_DS_APP, threeDsApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SCA_SENDS_APP, scaSendsApp);
        context.getStore(GLOBAL).put(InnovatorSetup.LOW_VALUE_EXEMPTION_APP, lowValueExemptionApp);
        context.getStore(GLOBAL).put(InnovatorSetup.PASSCODE_APP, passcodeApp);

        context.getStore(GLOBAL).put(InnovatorSetup.SEMI_PASSCODE_APP, semiPasscodeApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SEMI_SCA_SENDS_APP, semiScaSendsApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SEMI_SCA_PASSCODE_APP, semiScaPasscodeApp);
        context.getStore(GLOBAL).put(InnovatorSetup.SECONDARY_SCA_APP, secondaryScaApp);

        context.getStore(GLOBAL).put(InnovatorSetup.APPLICATION_ONE_UK, applicationOneUk);

        this.store = context.getStore(GLOBAL);
    }

    public void testPlanExecutionFinished(TestPlan testPlan) {

        if (DB_CONNECTION != null) {
            try {
                DB_CONNECTION.close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void setUpInnovator() throws IOException {

        final String testingEnvironment = ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment();

        applications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_multi_configuration.json",
                                        testingEnvironment)),
                                ProgrammeDetailsModel[].class));

        applicationOne = filterApplications(applications, "ApplicationOne");
        applicationTwo = filterApplications(applications, "ApplicationTwo");
        applicationThree = filterApplications(applications, "ApplicationThree");
        applicationFour = filterApplications(applications, "ApplicationFour");
        oddApp = filterApplications(applications, "OddApp");
        scaApp = filterApplications(applications, "ScaApp");
        webhooksApp = filterApplications(applications, "WebhooksApp");
        scaMaApp = filterApplications(applications, "ScaMaApp");
        scaMcApp = filterApplications(applications, "ScaMcApp");
        webhooksOddApp = filterApplications(applications, "WebhooksOddApp");
        scaEnrolApp = filterApplications(applications, "ScaEnrolApp");
        threeDsApp = filterApplications(applications, "ThreeDSApp");
        scaSendsApp = filterApplications(applications, "ScaSendsApp");
        lowValueExemptionApp = filterApplications(applications, "LowValueExemptionApp");
        passcodeApp = filterApplications(applications, "PasscodeApp");
        secondaryScaApp = filterApplications(applications, "SecondaryScaApp");

        nonFpsApplications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_multi_nonfps_configuration.json",
                                testingEnvironment)),
                                ProgrammeDetailsModel[].class));

        nonFpsEnabledTenantDetails = nonFpsApplications.stream().filter(x -> x.getProgrammeName().equals("ApplicationOne")).collect(Collectors.toList()).get(0);

        semiApplications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_semi_configuration.json",
                                        testingEnvironment)),
                                ProgrammeDetailsModel[].class));

        semiPasscodeApp = filterApplications(semiApplications, "PasscodeApp");
        semiScaSendsApp = filterApplications(semiApplications, "ScaSendsApp");
        semiScaPasscodeApp = filterApplications(semiApplications, "ScaPasscodeApp");

        ukApplications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_multi_uk_configuration.json",
                                        testingEnvironment)),
                                ProgrammeDetailsModel[].class));

        applicationOneUk = filterApplications(ukApplications, "ApplicationOneUk");
    }

    private ProgrammeDetailsModel filterApplications(final List<ProgrammeDetailsModel> applications,
                                                     final String applicationName){
        return applications.stream().filter(x -> x.getProgrammeName().equals(applicationName)).collect(Collectors.toList()).get(0);
    }

    private void updateSecurityModel(final List<ProgrammeDetailsModel> applications){

        if (ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment().equals("qa")){
            applications.forEach(application -> {
                try {
                    ProgrammeDatabaseHelper.setDefaultSecurityModel(application.getProgrammeId());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if (!application.getProgrammeName().contains("Webhooks")) {
                    try {
                        ProgrammeDatabaseHelper.updateProgrammeWebhook(true, application.getProgrammeId());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            applications.stream().filter(x -> !x.getProgrammeName().contains("Webhooks")).forEach(application -> {

                final Map<String, Boolean> securityModelConfig =
                        ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                                SecurityModelConfiguration.PASSWORD.name(), false,
                                SecurityModelConfiguration.CVV.name(), true,
                                SecurityModelConfiguration.CARD_NUMBER.name(), true);

                final UpdateProgrammeModel updateProgrammeModel =
                        UpdateProgrammeModel.builder()
                                .setWebhookDisabled(true)
                                .setSecurityModelConfig(securityModelConfig)
                                .build();

                TestHelper.ensureAsExpected(15,
                        () -> AdminService.updateProgramme(updateProgrammeModel, application.getProgrammeId(), AdminService.loginAdmin()),
                        SC_OK);
            });
        }
    }
}