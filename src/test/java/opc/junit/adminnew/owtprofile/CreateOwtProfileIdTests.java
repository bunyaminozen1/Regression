package opc.junit.adminnew.owtprofile;

import opc.models.admin.OwtProgrammes;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class CreateOwtProfileIdTests extends BaseOwtProfileSetup {
    @Test
    public void CreateOwtProfile_SkipWithdrawalScaTrue_Success() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .payletTypeCode("default_owts")
                .code("default_owts")
                .skipWithdrawalSca(true)
                .build();
        AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, programmeId)
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
                .body("fee[0].fees[0].fee.type", equalTo("FLAT"))
                .body("blockWithdrawalToExternalAccounts", equalTo(false));
    }

    @Test
    public void CreateOwtProfile_BlockWithdrawalToExternalAccountsTrue_Success() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .payletTypeCode("default_owts")
                .code("default_owts")
                .blockWithdrawalToExternalAccounts(true)
                .build();
        AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, programmeId)
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
                .body("fee[0].fees[0].fee.type", equalTo("FLAT"))
                .body("blockWithdrawalToExternalAccounts", equalTo(false));
    }

    @Test
    public void CreateOwtProfile_FlagsAreNull_Success() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .blockWithdrawalToExternalAccounts(null)
                .skipWithdrawalSca(null)
                .payletTypeCode("default_owts")
                .code("default_owts")
                .build();
        AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, programmeId)
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
                .body("skipWithdrawalSca", equalTo(false));

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
                .body("fee[0].fees[0].fee.type", equalTo("FLAT"))
                .body("blockWithdrawalToExternalAccounts", equalTo(false))
                .body("skipWithdrawalSca", equalTo(false));
    }

    @Test
    public void CreateOwtProfile_CodeIsNull_BadRequest() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .code(null)
                .payletTypeCode("default_owts")
                .build();
        AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, programmeId)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.code: must not be blank"));
    }

    @Test
    public void CreateOwtProfile_PayletTypeCodeIsNull_BadRequest() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .payletTypeCode(null)
                .code("default_owts")
                .code(RandomStringUtils.randomAlphabetic(6))
                .build();
        AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, programmeId)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.payletTypeCode: must not be blank"));
    }

    @Test
    public void CreateOwtProfile_OwtProfileNotExists_NotFound() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .blockWithdrawalToExternalAccounts(null)
                .skipWithdrawalSca(null)
                .payletTypeCode("default_owts")
                .code("default_owts")
                .build();
        AdminService.createOWTProfileId(adminTenantToken, owtProgrammes, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CreateOwtProfile_AuthTokenMissing_Unauthorized() {
        final OwtProgrammes owtProgrammes = OwtProgrammes.builder()
                .payletTypeCode("default_owts")
                .code("default_owts")
                .blockWithdrawalToExternalAccounts(null)
                .skipWithdrawalSca(null)
                .build();
        AdminService.createOWTProfileId("", owtProgrammes, programmeId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

}
