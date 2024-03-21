package fpi.admin.programme;

import fpi.admin.BaseAdminSetup;
import fpi.paymentrun.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ExportProfilesTests extends BaseAdminSetup {

    @Test
    public void ExportProfiles_ValidProgrammeId_Success(){
        AdminService.exportProfiles(programmeId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("programmeId", equalTo(programmeId))
                .body("companyRegistrationName", equalTo(innovatorName))
                .body("corporateProfileId", equalTo(corporateProfileId))
                .body("linkedAccountProfileId", equalTo(linkedManagedAccountProfileId))
                .body("managedAccountProfileId", equalTo(zeroBalanceManagedAccountProfileId))
                .body("openBankingRedirectUrl", notNullValue())
                .body("owtProfileId", equalTo(paymentOwtProfileId))
                .body("withdrawProfileId", equalTo(sweepOwtProfileId));
    }

    @Test
    public void ExportProfiles_InvalidProgrammeId_ProgrammeNotFound(){
        AdminService.exportProfiles(RandomStringUtils.randomAlphabetic(18), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
    }

    @Test
    public void ExportProfiles_EmptyProgrammeId_NotFound(){
        AdminService.exportProfiles("", adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ExportProfiles_InvalidToken_Unauthorised(){
        AdminService.exportProfiles(programmeId, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ExportProfiles_EmptyToken_Unauthorised(){
        AdminService.exportProfiles(programmeId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ExportProfiles_NullToken_Unauthorised(){
        AdminService.exportProfiles(programmeId, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
