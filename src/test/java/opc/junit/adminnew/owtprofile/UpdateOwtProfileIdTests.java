package opc.junit.adminnew.owtprofile;

import opc.models.admin.OwtProgrammes;
import opc.services.adminnew.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Execution(ExecutionMode.SAME_THREAD)
public class UpdateOwtProfileIdTests extends BaseOwtProfileSetup {

    private static OwtProgrammes owtProgrammes;
    private static String profileId;

    @BeforeEach
    public void BeforeEach() {
        owtProgrammes = OwtProgrammes.builder()
                .payletTypeCode("default_owts")
                .code("default_owts")
                .build();
        profileId = AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, programmeId)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("profile.id");
    }

    @Test
    public void UpdateProfile_SkipWithdrawalScaTrue_Success() {
        final OwtProgrammes owtProgrammesUpdate = OwtProgrammes.builder()
                .payletTypeCode("default_owts")
                .code("default_owts")
                .skipWithdrawalSca(true)
                .build();

        AdminService.updateOWTProfileId(adminTenantToken, owtProgrammesUpdate, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("blockWithdrawalToExternalAccounts", equalTo(false))
                .body("profile.active", equalTo(true))
                .body("profile.code", equalTo("default_owts"))
                .body("profile.id", notNullValue())
                .body("profile.payletKey", equalTo("outgoing_wire_transfers"))
                .body("profile.payletTypeCode", equalTo("default_owts"))
                .body("profile.programmeId", equalTo(programmeId))
                .body("returnOwtFee", equalTo(false))
                .body("skipWithdrawalSca", equalTo(true));

        AdminService.getOWTProfileId(adminTenantToken, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("profile.id", equalTo(profileId))
                .body("profile.programmeId", equalTo(programmeId))
                .body("profile.code", notNullValue())
                .body("profile.payletTypeCode", notNullValue())
                .body("profile.payletKey", notNullValue())
                .body("blockWithdrawalToExternalAccounts", equalTo(false))
                .body("skipWithdrawalSca", equalTo(true));
    }

    @Test
    public void UpdateOWTProfile_BlockWithdrawalToExternalAccountsTrue_Success() {
        final OwtProgrammes owtProgrammesUpdate = OwtProgrammes.builder()
                .blockWithdrawalToExternalAccounts(true)
                .payletTypeCode("default_owts")
                .code("default_owts")
                .build();
        AdminService.updateOWTProfileId(adminTenantToken, owtProgrammesUpdate, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("blockWithdrawalToExternalAccounts", equalTo(true))
                .body("profile.active", equalTo(true))
                .body("profile.code", equalTo("default_owts"))
                .body("profile.id", notNullValue())
                .body("profile.payletKey", equalTo("outgoing_wire_transfers"))
                .body("profile.payletTypeCode", equalTo("default_owts"))
                .body("profile.programmeId", equalTo(programmeId))
                .body("returnOwtFee", equalTo(false))
                .body("skipWithdrawalSca", equalTo(false));

        AdminService.getOWTProfileId(adminTenantToken, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("profile.id", equalTo(profileId))
                .body("profile.programmeId", equalTo(programmeId))
                .body("profile.code", notNullValue())
                .body("profile.payletTypeCode", notNullValue())
                .body("profile.payletKey", notNullValue())
                .body("blockWithdrawalToExternalAccounts", equalTo(true))
                .body("skipWithdrawalSca", equalTo(false));
    }

    @Test
    public void UpdateOWTProfileId_FlagsAreNotNull_Success() {
        final OwtProgrammes owtProgrammesUpdate = OwtProgrammes.builder()
                .skipWithdrawalSca(false)
                .blockWithdrawalToExternalAccounts(true)
                .build();
        AdminService.updateOWTProfileId(adminTenantToken, owtProgrammesUpdate, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("blockWithdrawalToExternalAccounts", equalTo(true))
                .body("profile.active", equalTo(true))
                .body("profile.code", equalTo("default_owts"))
                .body("profile.id", notNullValue())
                .body("profile.payletKey", equalTo("outgoing_wire_transfers"))
                .body("profile.payletTypeCode", equalTo("default_owts"))
                .body("profile.programmeId", equalTo(programmeId))
                .body("returnOwtFee", equalTo(false))
                .body("skipWithdrawalSca", equalTo(false));

        AdminService.getOWTProfileId(adminTenantToken, programmeId, profileId)
                .then()
                .statusCode(SC_OK)
                .body("profile.id", equalTo(profileId))
                .body("profile.programmeId", equalTo(programmeId))
                .body("profile.code", notNullValue())
                .body("profile.payletTypeCode", notNullValue())
                .body("profile.payletKey", notNullValue())
                .body("blockWithdrawalToExternalAccounts", equalTo(true))
                .body("skipWithdrawalSca", equalTo(false));
    }


    @Test
    public void CreateOwtProfile_AuthTokenMissing_Unauthorized() {
        final OwtProgrammes owtProgrammesUpdate = OwtProgrammes.builder()
                .skipWithdrawalSca(true)
                .build();
        AdminService.updateOWTProfileId("", owtProgrammesUpdate, programmeId, owtProfileId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
