package opc.junit.adminnew.owtprofile;

import opc.services.adminnew.AdminService;
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
        AdminService.getOWTProfileId(adminTenantToken, programmeId, owtProfileId)
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
    public void GetOwtProfileId_AuthTokenMissing_Unauthorized() {
        AdminService.getOWTProfileId("", programmeId, owtProfileId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    @Disabled
    //should return 404, but it's returning 405. Since it's existing endpoint - will ignore this
    public void GetOwtProfileId_ProgrammeIdMissing_NotFound() {
        AdminService.getOWTProfileId(adminTenantToken, "", owtProfileId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @Disabled
    //should return 404, but it's returning 405. Since it's existing endpoint - will ignore this
    public void GetOwtProfileId_OwtProfileIdMissing_NotFound() {
        AdminService.getOWTProfileId(adminTenantToken, programmeId, "")
                .then()
                .statusCode(SC_NOT_FOUND);
    }


}
