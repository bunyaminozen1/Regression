package opc.junit.admin.corporates;

import opc.enums.opc.IdentityType;
import opc.enums.opc.PluginStatus;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.CreatePluginModel;
import opc.models.innovator.CreateCorporateProfileModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static opc.junit.helpers.innovator.InnovatorHelper.createCorporateProfileWithPassword;
import static opc.services.innovatornew.InnovatorService.linkPluginToProgramme;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetCorporateTests extends BaseCorporatesSetup{

    @Test
    public void GetCorporate_WithoutImpersonatedToken_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AdminService.getCorporate(adminToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("id.id", equalTo(corporate.getLeft()))
                .body("tenantId", equalTo(Integer.parseInt(tenantId)))
                .body("programmeId", equalTo(programmeId));
    }

    @Test
    public void GetCorporate_withPluginData_Success() {

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        createPluginModel.setStatus(PluginStatus.ACTIVE);
        createPluginModel.setAdminUrl("www.adminUrl.com");

        String pluginCode = AdminService.createPlugin(createPluginModel, adminToken)
                .jsonPath().getString("code");

        linkPluginToProgramme(innovatorToken, programmeId, pluginCode);

        final CreateCorporateProfileModel createCorporateProfileModel = CreateCorporateProfileModel.DefaultCreateCorporateProfileModel().build();
        String corpProfileId = createCorporateProfileWithPassword(createCorporateProfileModel, innovatorToken, programmeId);

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corpProfileId, secretKey);
        String corpId = corporate.getLeft();

        var pluginData = AdminService.getCorporate(adminToken, corpId).body().jsonPath().getList("pluginData.pluginData.pluginCode");
        boolean containsPluginData = pluginData.contains(pluginCode);
        assertTrue(containsPluginData);
    }

    @Test
    public void GetCorporate_ImpersonatedToken_Success(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AdminService.getCorporate(impersonatedAdminToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("id.id", equalTo(corporate.getLeft()))
                .body("tenantId", equalTo(Integer.parseInt(tenantId)))
                .body("programmeId", equalTo(programmeId));
    }
}
