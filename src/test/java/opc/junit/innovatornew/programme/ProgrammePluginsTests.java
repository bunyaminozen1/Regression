package opc.junit.innovatornew.programme;

import io.restassured.response.Response;
import opc.enums.opc.PluginStatus;
import opc.models.admin.CreatePluginModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.services.adminnew.AdminService;
import org.junit.jupiter.api.Test;

import static opc.services.admin.AdminService.createPlugin;
import static opc.services.innovatornew.InnovatorService.getPluginsOnProgramme;
import static opc.services.innovatornew.InnovatorService.linkPluginToProgramme;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class ProgrammePluginsTests extends BaseProgrammeSetup {
    @Test
    public void LinkPluginToProgramme_HappyPath_Success() {

        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();
        final Response response = AdminService.createProgramme(tenantAdminToken, createProgrammeModel);
        final String programmeId = response.jsonPath().get("id");
        final String modelId = response.jsonPath().get("modelId");

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);
        createPluginModel.setModelId(modelId);
        final String pluginId = createPlugin(createPluginModel, adminToken).jsonPath().get("id");

        linkPluginToProgramme(innovatorToken, programmeId, pluginId)
                .then()
                .statusCode(SC_OK)
                .body("pluginId", equalTo(pluginId))
                .body("programmeId", equalTo(programmeId));

        getPluginsOnProgramme(innovatorToken, programmeId)
                .then()
                .statusCode(SC_OK)
                .body("installedPlugin[0].pluginId", equalTo(pluginId))
                .body("installedPlugin[0].programmeId", equalTo(programmeId));
    }

    @Test
    public void LinkPluginToProgramme_LinkingPluginToMultipleProgrammes_Success() {

        final CreateProgrammeModel createProgrammeModelOne = CreateProgrammeModel.InitialProgrammeModel();
        final Response responseOne = AdminService.createProgramme(tenantAdminToken, createProgrammeModelOne);
        final String programmeIdOne = responseOne.jsonPath().get("id");
        final String modelId = responseOne.jsonPath().get("modelId");

        final CreateProgrammeModel createProgrammeModelTwo = CreateProgrammeModel.InitialProgrammeModel();
        final Response responseTwo = AdminService.createProgramme(tenantAdminToken, createProgrammeModelTwo);
        final String programmeIdTwo = responseTwo.jsonPath().get("id");

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);
        createPluginModel.setModelId(modelId);
        final String pluginId = createPlugin(createPluginModel, adminToken).jsonPath().get("id");

        linkPluginToProgramme(innovatorToken, programmeIdOne, pluginId)
                .then()
                .statusCode(SC_OK)
                .body("pluginId", equalTo(pluginId))
                .body("programmeId", equalTo(programmeIdOne));

        linkPluginToProgramme(innovatorToken, programmeIdTwo, pluginId)
                .then()
                .statusCode(SC_OK)
                .body("pluginId", equalTo(pluginId))
                .body("programmeId", equalTo(programmeIdTwo));

        getPluginsOnProgramme(innovatorToken, programmeIdOne)
                .then()
                .statusCode(SC_OK)
                .body("installedPlugin[0].pluginId", equalTo(pluginId))
                .body("installedPlugin[0].programmeId", equalTo(programmeIdOne));

        getPluginsOnProgramme(innovatorToken, programmeIdTwo)
                .then()
                .statusCode(SC_OK)
                .body("installedPlugin[0].pluginId", equalTo(pluginId))
                .body("installedPlugin[0].programmeId", equalTo(programmeIdTwo));
    }

    @Test
    public void LinkPluginToProgramme_InactivePlugin_Conflict() {

        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();
        final Response response = AdminService.createProgramme(tenantAdminToken, createProgrammeModel);
        final String programmeId = response.jsonPath().get("id");
        final String modelId = response.jsonPath().get("modelId");

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.INACTIVE);
        createPluginModel.setModelId(modelId);
        final String pluginId = createPlugin(createPluginModel, adminToken).jsonPath().get("id");

        linkPluginToProgramme(innovatorToken, programmeId, pluginId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PLUGIN_NOT_ACTIVE"));
    }

    @Test
    public void LinkPluginToProgramme_InvalidModelId_Conflict() {

        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();
        final Response response = AdminService.createProgramme(tenantAdminToken, createProgrammeModel);
        final String programmeId = response.jsonPath().get("id");
        final String modelId = "99";

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);
        createPluginModel.setModelId(modelId);
        final String pluginId = createPlugin(createPluginModel, adminToken).jsonPath().get("id");

        linkPluginToProgramme(innovatorToken, programmeId, pluginId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROGRAMME_MODEL_ID_ERROR"));
    }

    @Test
    public void LinkPluginToProgramme_LinkPluginTwice_Conflict() {

        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();
        final Response response = AdminService.createProgramme(tenantAdminToken, createProgrammeModel);
        final String programmeId = response.jsonPath().get("id");
        final String modelId = response.jsonPath().get("modelId");

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);
        createPluginModel.setModelId(modelId);
        final String pluginId = createPlugin(createPluginModel, adminToken).jsonPath().get("id");

        linkPluginToProgramme(innovatorToken, programmeId, pluginId)
                .then()
                .statusCode(SC_OK)
                .body("pluginId", equalTo(pluginId))
                .body("programmeId", equalTo(programmeId));

        linkPluginToProgramme(innovatorToken, programmeId, pluginId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ALREADY_INSTALLED"));
    }
}
