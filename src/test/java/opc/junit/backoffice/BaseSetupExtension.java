package opc.junit.backoffice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mysql.cj.jdbc.ConnectionImpl;
import commons.config.ConfigHelper;
import opc.database.BaseDatabaseExtension;
import opc.database.DatabaseConnection;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.SecurityModelConfiguration;
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

    private static ProgrammeDetailsModel applicationOne;
    private static ProgrammeDetailsModel nonFpsEnabledTenantDetails;

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {

        final String uniqueKey = this.getClass().getName();

        setUpInnovator();

        if (!INITIALISED) {
            INITIALISED = true;
            DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            context.getRoot().getStore(GLOBAL).put(uniqueKey, this);
            updateSecurityModel();
        }

        if (DB_CONNECTION != null) {
            if (((ConnectionImpl) DB_CONNECTION).getSession().isClosed()) {
                DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            }
        }

        context.getStore(GLOBAL).put(InnovatorSetup.APPLICATION_ONE, applicationOne);
        context.getStore(GLOBAL).put(InnovatorSetup.NON_FPS_ENABLED_TENANT, nonFpsEnabledTenantDetails);

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

        final List<ProgrammeDetailsModel> applications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_bo_configuration.json",
                                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())),
                                ProgrammeDetailsModel[].class));

        applicationOne = applications.stream().filter(x -> x.getProgrammeName().equals("ApplicationOne")).collect(Collectors.toList()).get(0);

        final List<ProgrammeDetailsModel> nonFpsApplications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_multi_nonfps_configuration.json",
                                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())),
                                ProgrammeDetailsModel[].class));

        nonFpsEnabledTenantDetails = nonFpsApplications.stream().filter(x -> x.getProgrammeName().equals("ApplicationOne")).collect(Collectors.toList()).get(0);
    }

    private void updateSecurityModel() {
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
                () -> AdminService.updateProgramme(updateProgrammeModel, applicationOne.getProgrammeId(), AdminService.loginAdmin()),
                SC_OK);

        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateProgramme(updateProgrammeModel, nonFpsEnabledTenantDetails.getProgrammeId(), AdminService.loginAdmin()),
                SC_OK);
    }
}