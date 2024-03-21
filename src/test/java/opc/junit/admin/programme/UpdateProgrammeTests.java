package opc.junit.admin.programme;

import opc.models.admin.UpdateProgrammeModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_OK;

public class UpdateProgrammeTests extends BaseProgrammeSetup {

    @AfterAll
    public static void setDefaultJurisdictions(){
        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder().setJurisdictions(List.of("EEA", "UK")).build();
        AdminService.updateProgramme(updateProgrammeModel, programmeId, tenantAdminToken)
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @MethodSource("jurisdictionProvider")
    public void UpdateProgramme_UpdateJurisdictionField_Success(final List<String> jurisdictions) {
        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder().setJurisdictions(jurisdictions).build();
        final List<String> updatedJurisdictions = AdminService.updateProgramme(updateProgrammeModel, programmeId, tenantAdminToken)
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
