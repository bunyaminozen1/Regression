package opc.junit.innovator.programme;

import opc.models.innovator.UpdateProgrammeModel;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

public class UpdateProgrammeTests extends BaseProgrammeSetup {
    final private static String script = "112>*<script>alert()</script>";

    @AfterAll
    public static void setDefaultJurisdictions(){
        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder().setJurisdictions(List.of("EEA", "UK")).build();
        InnovatorService.updateProgramme(updateProgrammeModel, programmeId, innovatorToken)
                .then()
                .statusCode(SC_OK);
    }

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

    @ParameterizedTest
    @MethodSource("jurisdictionProvider")
    public void UpdateProgramme_UpdateJurisdictionField_Success(final List<String> jurisdictions) {
        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder().setJurisdictions(jurisdictions).build();
        final List<String> updatedJurisdictions = InnovatorService.updateProgramme(updateProgrammeModel, programmeId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("jurisdictions");

        Assertions.assertEquals(updatedJurisdictions, jurisdictions);
    }

    static Stream<Arguments> jurisdictionProvider() {
        return Stream.of(
                Arguments.of(List.of("EEA")),
                Arguments.of(List.of("UK")),
                Arguments.of(List.of("UNKNOWN")),
                Arguments.of(List.of("EEA", "UK")),
                Arguments.of(List.of("EEA", "UK", "UNKNOWN"))
        );
    }
}
