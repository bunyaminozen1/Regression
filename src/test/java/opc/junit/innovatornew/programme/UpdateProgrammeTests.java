package opc.junit.innovatornew.programme;

import opc.models.innovator.UpdateProgrammeModel;
import opc.services.innovatornew.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

public class UpdateProgrammeTests extends BaseProgrammeSetup {
    final private static String script = "112>*<script>alert()</script>";

    @Test
    public void UpdateProgramme_HappyPath_Success() {

        final UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setName(RandomStringUtils.randomAlphabetic(10))
                        .setWebhookDisabled(true)
                        .setAuthForwardingEnabled(false)
                        .build();

        InnovatorService.updateProgramme(updateProgrammeModel, programmeId, innovatorToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void UpdateProgramme_ScriptInName_BadRequest() {

        final UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setName(script)
                        .setWebhookDisabled(true)
                        .setAuthForwardingEnabled(false)
                        .build();

        InnovatorService.updateProgramme(updateProgrammeModel, programmeId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
