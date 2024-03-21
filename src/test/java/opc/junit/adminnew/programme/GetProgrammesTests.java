package opc.junit.adminnew.programme;

import opc.junit.helpers.adminnew.AdminHelper;
import opc.models.admin.GetInnovatorsModel;
import opc.models.admin.PagingLimitModel;
import opc.services.adminnew.AdminService;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;

public class GetProgrammesTests extends BaseProgrammeSetup {
    @Test
    public void GetProgrammes_HappyPath_Success() {
        PagingLimitModel paging = new PagingLimitModel().setLimit(10);

        AdminService.getProgrammes(paging, adminToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetModels_HappyPath_Success() {
        PagingLimitModel paging = new PagingLimitModel().setLimit(10);

        AdminService.getModels(paging, adminToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetModel_HappyPath_Success() {
        AdminService.getModel(adminToken, AdminHelper.getModelIdProgramme(adminToken))
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetInnovatorsByModelId_HappyPath_Success() {
        GetInnovatorsModel innovatorsModel = GetInnovatorsModel.defaultGetInnovatorsModel(AdminHelper.getModelIdProgramme(adminToken));

        AdminService.getInnovators(innovatorsModel, adminToken)
                .then()
                .statusCode(SC_OK);
    }
}
