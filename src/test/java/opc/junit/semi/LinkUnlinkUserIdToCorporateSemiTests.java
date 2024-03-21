package opc.junit.semi;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.UserId;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.admin.AdminService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinkUnlinkUserIdToCorporateSemiTests extends BaseSemiSetup {
    /**
     * SEMI flow for Link and Unlink endpoints. Those endpoints are used to directly link or unlink user. User has to be KYC verified.
     */
    private static String adminImpersonatedTenantToken;

    @BeforeAll
    public static void BeforeAll() {
        adminImpersonatedTenantToken = AdminService.impersonateTenant(tenantId, AdminService.loginAdmin());
    }

    @Test
    void LinkUserIdToCorporate_UserLinked_Success() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(secretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

    }

    @Test
    void LinkUserIdToCorporate_RandomUserName_Conflict() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New identity linked to root user with the random name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(RandomStringUtils.randomAlphabetic(10),
                        RandomStringUtils.randomAlphabetic(10));
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKey);
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("USER_DATA_DO_NOT_MATCH"));
    }

    @Test
    void LinkUserIdToCorporate_UserAlreadyLinked_Conflict() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(secretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

        //New user linked to root user again
        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("USER_ALREADY_LINKED_TO_IDENTITY"));
    }

    @Test
    void LinkUserIdToCorporate_RootUserNotVerified_Conflict() {
        //Root user is created and email not verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();

        //New identity linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(name, surname);

        final String corporateUserId = CorporatesHelper.createCorporateWithPassword(createCorporateUser, secretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void LinkUserIdToCorporate_RandomUser_Conflict() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New random user id linked to the root user
        final String corporateUserId = RandomStringUtils.randomNumeric(10);
        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("USER_NOT_FOUND"));
    }

    @Test
    void LinkUserIdToCorporate_NoApi_Unauthorized() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    void UnlinkUserIdFromCorporate_UserUnlinked_Success() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(secretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

        //Unlink user from root user
        AdminService.unlinkUseridFromCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        final String corporateUserUnlinkedEmail = CorporatesService.getCorporates(secretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateUserEmail, corporateUserUnlinkedEmail);
    }

    @Test
    void UnlinkUserIdFromCorporate_UserIsNotLinked_Conflict() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKey);

        AdminService.unlinkUseridFromCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("NO_LINK_TO_REVERT"));
    }

    @Test
    void UnlinkUserIdFromCorporate_RandomUser_Conflict() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New random user id linked to the root user
        final String corporateUserId = RandomStringUtils.randomNumeric(10);
        AdminService.unlinkUseridFromCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("USER_NOT_FOUND"));
    }

    @Test
    void LinkUserIdToCorporate_ProfileMismatchSameProgramme_Conflict() {

        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //different corporate profileId
        final String corporatesProfileIdTwo = "111809901968556321";

        //New user with the same name and surname created under application two
        final CreateCorporateModel createCorporateUser =
                CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileIdTwo)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setName(name)
                                .setSurname(surname).build()).build();

        final Pair<String, String> authenticatedCorporateUser = CorporatesHelper.createAuthenticatedCorporate(createCorporateUser, "fonXgKVylBQBjTqDa1kBHw==", "Pass1234");
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = authenticatedCorporateUser.getLeft();

        CorporatesHelper.verifyEmail(corporateUserEmail, "fonXgKVylBQBjTqDa1kBHw==");
        CorporatesHelper.verifyKyb("fonXgKVylBQBjTqDa1kBHw==", corporateUserId);

        //Link new user to corporate root user
        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("PROFILE_MISMATCH"));

    }

    @Test
    void LinkUserIdToCorporate_ProfileMismatchDifferentProgrammes_Conflict() {

        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, passcodeAppSecretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, passcodeAppSecretKey);

        //New user with the same name and surname created under application two
        final CreateCorporateModel createCorporateUser =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setName(name)
                                .setSurname(surname).build()).build();

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, secretKeyScaSendsApp);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();

        CorporatesHelper.verifyEmail(corporateUserEmail, secretKeyScaSendsApp);
        CorporatesHelper.verifyKyb(secretKeyScaSendsApp, corporateUserId);

        //Link new user to corporate root user
        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("USER_NOT_FOUND"));

    }


    private static CreateCorporateModel createCorporateUser(String name, String surname) {
        return
                CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setName(name)
                                .setSurname(surname).build()).build();
    }
}
