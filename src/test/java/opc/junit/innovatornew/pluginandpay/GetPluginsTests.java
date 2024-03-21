package opc.junit.innovatornew.pluginandpay;

import opc.enums.opc.PluginStatus;
import opc.junit.helpers.admin.AdminHelper;
import opc.models.admin.CreatePluginModel;
import opc.models.admin.CreatePluginResponseModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static opc.services.innovatornew.InnovatorService.getPluginById;
import static opc.services.innovatornew.InnovatorService.getPluginByName;
import static opc.services.innovatornew.InnovatorService.getPlugins;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

public class GetPluginsTests extends BasePluginAndPaySetup {
    @Test
    public void GetPlugins_Success() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);

        final CreatePluginResponseModel plugin = AdminHelper.createPlugin(createPluginModel, adminToken);
        final int location = getPlugins(innovatorToken).jsonPath().getList("plugins.id").indexOf(plugin.getId());

        getPlugins(innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("plugins["+location+"].id", equalTo(plugin.getId()))
                .body("plugins["+location+"].name", equalTo(createPluginModel.getName()))
                .body("plugins["+location+"].description", equalTo(createPluginModel.getDescription()))
                .body("plugins["+location+"].icon", equalTo(createPluginModel.getIcon()))
                .body("plugins["+location+"].fpiKey", equalTo(plugin.getFpiKey()))
                .body("plugins["+location+"].webhookUrl", equalTo(createPluginModel.getWebhookUrl()));
    }

    @ParameterizedTest
    @EnumSource(value = PluginStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"ACTIVE","UNKNOWN"})
    public void GetPlugins_NonActive_NotFound(final PluginStatus status) {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(status);

        final CreatePluginResponseModel plugin = AdminHelper.createPlugin(createPluginModel, adminToken);
        final List<Long> listId = getPlugins(innovatorToken).jsonPath().getList("plugins.id");

        if(listId.contains(plugin.getId()))
            fail("Plugin which is "+ status +" should not be visible");
    }

    @Test
    public void GetPluginById_Success() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);

        final CreatePluginResponseModel plugin = AdminHelper.createPlugin(createPluginModel, adminToken);

        getPluginById(String.valueOf(plugin.getId()), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("plugins[0].id", equalTo(plugin.getId()))
                .body("plugins[0].name", equalTo(createPluginModel.getName()))
                .body("plugins[0].description", equalTo(createPluginModel.getDescription()))
                .body("plugins[0].icon", equalTo(createPluginModel.getIcon()))
                .body("plugins[0].fpiKey", equalTo(plugin.getFpiKey()))
                .body("plugins[0].webhookUrl", equalTo(createPluginModel.getWebhookUrl()));
    }

    @Test
    public void GetPluginById_EmptyList_Success() {
        final String pluginId = "11111111111";

        getPluginById(pluginId, innovatorToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPluginByCode_Success() {
        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);

        final CreatePluginResponseModel plugin = AdminHelper.createPlugin(createPluginModel, adminToken);

        getPluginByName(createPluginModel.getCode(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("plugins[0].id", equalTo(plugin.getId()))
                .body("plugins[0].name", equalTo(createPluginModel.getName()))
                .body("plugins[0].description", equalTo(createPluginModel.getDescription()))
                .body("plugins[0].icon", equalTo(createPluginModel.getIcon()))
                .body("plugins[0].fpiKey", equalTo(plugin.getFpiKey()))
                .body("plugins[0].webhookUrl", equalTo(createPluginModel.getWebhookUrl()));
    }

    @Test
    public void GetPluginByName_EmptyList_Success() {
        final String name = "UNKNOWN";

        getPluginByName(name, innovatorToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPlugins_InvalidToken_Unauthorized() {
        getPlugins(RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetPlugins_NoToken_Unauthorized() {
        getPlugins("")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
