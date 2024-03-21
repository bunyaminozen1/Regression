package opc.junit.adminnew.programme;

import opc.models.innovator.CreateProgrammeModel;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

public class CreateProgrammeTests extends BaseProgrammeSetup {

    @Test
    public void CreateProgrammeNewAdmin_ModelIdProvided_Success() {
        CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();

        AdminService.createProgramme(tenantAdminToken, createProgrammeModel)
                .then()
                .statusCode(SC_OK)
                .body("modelId", equalTo(String.valueOf(createProgrammeModel.getModelId())));
    }

    @Test
    public void CreateProgrammeNewAdmin_ModelIdNotProvided_BadRequest() {
        CreateProgrammeModel createProgrammeModel = new CreateProgrammeModel(RandomStringUtils.randomAlphabetic(7), RandomStringUtils.randomAlphabetic(7));

        AdminService.createProgramme(tenantAdminToken, createProgrammeModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.modelId: must not be blank"));
    }
}
