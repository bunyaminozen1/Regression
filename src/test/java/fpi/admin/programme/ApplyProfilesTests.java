package fpi.admin.programme;

import fpi.admin.BaseAdminSetup;
import fpi.paymentrun.models.admin.ApplyProfilesModel;
import fpi.paymentrun.services.admin.AdminService;
import opc.junit.helpers.innovator.InnovatorHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

public class ApplyProfilesTests extends BaseAdminSetup {
    private static String innovatorToken;
    private static String testProgrammeId;

    @BeforeAll
    public static void Setup(){
        innovatorToken =
                InnovatorHelper.registerLoggedInInnovator().getRight();
    }

    @BeforeEach
    public void ProgrammeSetup(){
        testProgrammeId = InnovatorHelper.createProgramme(innovatorToken).getLeft();
    }

    @Test
    public void ApplyProfiles_AllFields_Success() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .openBankingRedirectUrl("https://www.fake.com")
                .companyRegistrationName("Name")
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ApplyProfiles_OnlyRequiredFields_Success() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ApplyProfiles_ExistingProgramme_ProgrammeAlreadyExists() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(programmeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .openBankingRedirectUrl("https://www.fake.com")
                .companyRegistrationName("Name")
                .build();

        AdminService.applyProfiles(applyProfilesModel, programmeId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROGRAMME_ALREADY_EXISTS"));
    }

    @Test
    public void ApplyProfiles_InvalidProgrammeIdInModel_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(RandomStringUtils.randomAlphabetic(18))
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("programmeId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ApplyProfiles_InvalidCorporateProfileIdInModel_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomAlphabetic(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("corporateProfileId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ApplyProfiles_InvalidManagedAccountProfileIdInModel_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomAlphabetic(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("managedAccountProfileId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ApplyProfiles_InvalidOwtProfileIdInModel_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomAlphabetic(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("owtProfileId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ApplyProfiles_InvalidWithdrawProfileIdInModel_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomAlphabetic(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("withdrawProfileId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ApplyProfiles_InvalidLinkedAccountProfileIdInModel_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomAlphabetic(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linkedAccountProfileId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ApplyProfiles_WrongProgrammeId_ProgrammeNotFound() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
    }

    @Test
    public void ApplyProfiles_InvalidProgrammeId_BadRequest() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, RandomStringUtils.randomAlphanumeric(18), adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.programmeIds: must match \"^[0-9]+$\""));
    }

    @Test
    public void ApplyProfiles_EmptyProgrammeId_NotFound() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, "", adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ApplyProfiles_InvalidToken_Unauthorised() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ApplyProfiles_EmptyToken_Unauthorised() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ApplyProfiles_NullToken_Unauthorised() {
        ApplyProfilesModel applyProfilesModel = ApplyProfilesModel.builder()
                .programmeId(testProgrammeId)
                .corporateProfileId(RandomStringUtils.randomNumeric(18))
                .managedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .owtProfileId(RandomStringUtils.randomNumeric(18))
                .withdrawProfileId(RandomStringUtils.randomNumeric(18))
                .linkedAccountProfileId(RandomStringUtils.randomNumeric(18))
                .build();

        AdminService.applyProfiles(applyProfilesModel, testProgrammeId, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
