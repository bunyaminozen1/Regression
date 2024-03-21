package opc.junit.admin.programme;

import opc.models.innovator.CreateProgrammeModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

public class CreateProgrammeTests extends BaseProgrammeSetup {

    @Test
    public void CreateProgramme_ModelIdNotProvided_Success() {
        CreateProgrammeModel createProgrammeModel = new CreateProgrammeModel(RandomStringUtils.randomAlphabetic(7), RandomStringUtils.randomAlphabetic(7));

        AdminService.createProgramme(tenantAdminToken, createProgrammeModel)
                .then()
                .statusCode(SC_OK)
                .body("modelId", equalTo("0"));
    }

    @Test
    public void CreateProgramme_ModelIdProvided_Success() {
        CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();

        AdminService.createProgramme(tenantAdminToken, createProgrammeModel)
                .then()
                .statusCode(SC_OK)
                .body("modelId", equalTo(String.valueOf(createProgrammeModel.getModelId())));
    }

}
