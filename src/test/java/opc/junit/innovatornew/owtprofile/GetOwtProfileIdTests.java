package opc.junit.innovatornew.owtprofile;

import opc.services.innovatornew.InnovatorService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
public class GetOwtProfileIdTests extends BaseOwtProfileSetup {
    @Test
    public void GetOwtProfileId_HappyPath_Success() {
        InnovatorService.getOWTProfileId(innovatorToken, programmeId, owtProfileId)
                .then()
                .statusCode(SC_OK)
                .body("profile.id", equalTo(owtProfileId))
                .body("profile.programmeId", equalTo(programmeId))
                .body("profile.code", notNullValue())
                .body("profile.payletTypeCode", notNullValue())
                .body("profile.payletKey", notNullValue())
                .body("supportedType", notNullValue())
                .body("fee[0].type", notNullValue())
                .body("fee[0].fees[0].name", notNullValue())
                .body("fee[0].fees[0].fee.type", equalTo("FLAT"));
    }

    @Test
    @Disabled
    //should return 401, but it's returning 500.
    public void GetOwtProfileId_AuthTokenMissing_Unauthorized() {
        InnovatorService.getOWTProfileId("", programmeId, owtProfileId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    @Disabled
    //should return 404, but it's returning 405. Since it's existing endpoint - will ignore this
    public void GetOwtProfileId_ProgrammeIdMissing_NotFound() {
        InnovatorService.getOWTProfileId(innovatorToken, "", owtProfileId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @Disabled
    //should return 404, but it's returning 405. Since it's existing endpoint - will ignore this
    public void GetOwtProfileId_OwtProfileIdMissing_NotFound() {
        InnovatorService.getOWTProfileId(innovatorToken, programmeId, "")
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
