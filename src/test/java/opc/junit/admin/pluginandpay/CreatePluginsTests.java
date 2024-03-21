package opc.junit.admin.pluginandpay;

import opc.enums.opc.PluginStatus;
import opc.models.admin.CreatePluginModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static opc.services.admin.AdminService.createPlugin;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CreatePluginsTests extends BasePluginAndPaySetup {

    @ParameterizedTest
    @EnumSource(value = PluginStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void CreatePlugin_Success(final PluginStatus status) {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(status);

        AdminService.createPlugin(createPluginModel, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("name", equalTo(createPluginModel.getName()))
                .body("description", equalTo(createPluginModel.getDescription()))
                .body("status", equalTo(createPluginModel.getStatus().toString()))
                .body("icon", equalTo(createPluginModel.getIcon()))
                .body("fpiKey", notNullValue())
                .body("webhookUrl", equalTo(createPluginModel.getWebhookUrl()))
                .body("modelId", equalTo(createPluginModel.getModelId()));
    }

    @Test
    public void CreatePlugin_UnknownStatus_BadRequest() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.UNKNOWN);

        createPlugin(createPluginModel, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreatePlugin_InvalidToken_Unauthorized() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();

        createPlugin(createPluginModel, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePlugin_NoToken_Unauthorized() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();

        createPlugin(createPluginModel, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePlugin_SameCode_Conflict() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();

        createPlugin(createPluginModel, adminToken)
                .then()
                .statusCode(SC_OK);

        createPlugin(createPluginModel, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PLUGIN_CODE_NOT_UNIQUE"));
    }
}
