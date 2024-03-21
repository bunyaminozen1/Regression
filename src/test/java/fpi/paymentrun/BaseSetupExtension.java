package fpi.paymentrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mysql.cj.jdbc.ConnectionImpl;
import commons.config.ConfigHelper;
import commons.config.Configuration;
import opc.database.BaseDatabaseExtension;
import opc.database.DatabaseConnection;
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

    private static List<ProgrammeDetailsModel> applications;
    private static ProgrammeDetailsModel pluginsApplication;
    private static ProgrammeDetailsModel pluginsAppTwo;
    private static ProgrammeDetailsModel pluginsScaApp;
    private static ProgrammeDetailsModel pluginsScaMaApp;
    private static ProgrammeDetailsModel pluginsWebhooksApp;

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {

        final String uniqueKey = this.getClass().getName();

        setUpInnovator();

        if (!INITIALISED) {
            INITIALISED = true;
            DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            context.getRoot().getStore(GLOBAL).put(uniqueKey, this);
            updateSecurityModel(applications);
        }

        if (DB_CONNECTION != null) {
            if (((ConnectionImpl) DB_CONNECTION).getSession().isClosed()) {
                DB_CONNECTION = DatabaseConnection.getInstance().getConnection();
            }
        }

        context.getStore(GLOBAL).put(InnovatorSetup.PLUGINS_APP, pluginsApplication);
        context.getStore(GLOBAL).put(InnovatorSetup.PLUGINS_APP_TWO, pluginsAppTwo);
        context.getStore(GLOBAL).put(InnovatorSetup.PLUGINS_SCA_APP, pluginsScaApp);
        context.getStore(GLOBAL).put(InnovatorSetup.PLUGINS_SCA_MA_APP, pluginsScaMaApp);
        context.getStore(GLOBAL).put(InnovatorSetup.PLUGINS_WEBHOOKS_APP, pluginsWebhooksApp);

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

        final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();

        applications =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("./src/test/resources/TestConfiguration/%s_plugins_configuration.json",
                                        configuration.getMainTestEnvironment())),
                                ProgrammeDetailsModel[].class));

        pluginsApplication =
                applications.stream()
                        .filter(x -> x.getFpiKey().equals(configuration.getFpiKey()))
                        .filter(x -> x.getProgrammeName().equals("PluginsApp"))
                        .collect(Collectors.toList()).get(0);

        pluginsAppTwo =
                applications.stream()
                        .filter(x -> x.getFpiKey().equals(configuration.getFpiKey()))
                        .filter(x -> x.getProgrammeName().equals("PluginsAppTwo"))
                        .collect(Collectors.toList()).get(0);

        pluginsScaApp =
                applications.stream()
                        .filter(x -> x.getFpiKey().equals(configuration.getFpiKey()))
                        .filter(x -> x.getProgrammeName().equals("PluginsScaApp"))
                        .collect(Collectors.toList()).get(0);

        pluginsScaMaApp =
                applications.stream()
                        .filter(x -> x.getFpiKey().equals(configuration.getFpiKey()))
                        .filter(x -> x.getProgrammeName().equals("PluginsScaMaApp"))
                        .collect(Collectors.toList()).get(0);

        pluginsWebhooksApp =
                applications.stream()
                        .filter(x -> x.getFpiKey().equals(configuration.getFpiKey()))
                        .filter(x -> x.getProgrammeName().equals("PluginsWebhooksApp"))
                        .collect(Collectors.toList()).get(0);
    }

    private void updateSecurityModel(final List<ProgrammeDetailsModel> applications) {

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

            final String adminToken = AdminService.loginAdmin();

            TestHelper.ensureAsExpected(15,
                    () -> AdminService.updateProgramme(updateProgrammeModel, application.getProgrammeId(), adminToken),
                    SC_OK);

            final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();

            if (configuration.getTestRunEnvironment().equals("dev")) {

                TestHelper.ensureAsExpected(15,
                        () -> AdminService.updatePluginUrl(configuration.getPaymentRunWebhookUrl(),
                                adminToken, configuration.getPluginId()),
                        SC_OK);
            }
        });
    }
}
