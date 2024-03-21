package opc.junit.multi.modulr;

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

    private static List<ProgrammeDetailsModel> fiApplications;
    private static ProgrammeDetailsModel payneticsApp;
    private static ProgrammeDetailsModel payneticsModulrApp;
    private static ProgrammeDetailsModel modulrApp;
    private static ProgrammeDetailsModel multipleFisApp;

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {

        final String uniqueKey = this.getClass().getName();

        setUpInnovator();

        if (!INITIALISED) {
            INITIALISED = true;
            DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            context.getRoot().getStore(GLOBAL).put(uniqueKey, this);
            updateSecurityModel(fiApplications);
        }

        if (DB_CONNECTION != null) {
            if (((ConnectionImpl) DB_CONNECTION).getSession().isClosed()) {
                DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            }
        }

        context.getStore(GLOBAL).put(InnovatorSetup.PAYNETICS_APP, payneticsApp);
        context.getStore(GLOBAL).put(InnovatorSetup.PAYNETICS_MODULR_APP, payneticsModulrApp);
        context.getStore(GLOBAL).put(InnovatorSetup.MODULR_APP, modulrApp);
        context.getStore(GLOBAL).put(InnovatorSetup.MULTIPLE_FIS_APP, multipleFisApp);

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

        fiApplications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_fi_configuration.json",
                                        testingEnvironment)),
                                ProgrammeDetailsModel[].class));

        payneticsApp = filterApplications(fiApplications, "PayneticsApp");
        payneticsModulrApp = filterApplications(fiApplications, "PayneticsModulrApp");
        modulrApp = filterApplications(fiApplications, "ModulrApp");
        multipleFisApp = filterApplications(fiApplications, "MultipleFisApp");
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