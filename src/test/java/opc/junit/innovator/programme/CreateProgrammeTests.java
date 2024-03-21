package opc.junit.innovator.programme;

import opc.models.innovator.CreateProgrammeModel;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

public class CreateProgrammeTests extends BaseProgrammeSetup {

    @Test
    public void CreateProgrammeInnovator_ModelIdNotProvided_Success() {
        CreateProgrammeModel createProgrammeModel = new CreateProgrammeModel(RandomStringUtils.randomAlphabetic(7), RandomStringUtils.randomAlphabetic(7));

        InnovatorService.createProgramme(createProgrammeModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("modelId", equalTo("0"));
    }

    @Test
    public void CreateProgrammeInnovator_ModelIdProvided_Success() {
        CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();

        InnovatorService.createProgramme(createProgrammeModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("modelId", equalTo(String.valueOf(createProgrammeModel.getModelId())));
    }

    @Test
    public void CreateProgrammeNewInnovator_TokenNotProvided_Unauthorized() {
        CreateProgrammeModel createProgrammeModel = new CreateProgrammeModel(RandomStringUtils.randomAlphabetic(7), RandomStringUtils.randomAlphabetic(7));

        opc.services.innovatornew.InnovatorService.createProgramme(createProgrammeModel, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

}
