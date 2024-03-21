package opc.junit.semi;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.admin.UserId;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.Identity;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinkAuthUserSemiTests extends BaseSemiSetup {
    /**
     * SEMI flow for Link Auth User.
     * Main Case:
     * 1. Create Corp1 user
     * 2. Create Corp2 user linked to the Corp1
     * 3. Create Corp3 with ROOT and USER
     * 4. Link User corp3 to the Corp1 (admin endpoint)
     * => 3 users with same credentials
     * Ticket: https://weavr-payments.atlassian.net/browse/DEV-4558
     */

    private static final String PASSWORD = TestHelper.getDefaultPassword(secretKey);

    @Test
    void LinkAuthUser_UserLinked_Success() {
        //Root user #1 is created and email is verified
        final CreateCorporateModel createCorporateModelRootUser = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootUserEmail = createCorporateModelRootUser.getRootUser().getEmail();
        final String nameRootUser = createCorporateModelRootUser.getRootUser().getName();
        final String surnameRootUser = createCorporateModelRootUser.getRootUser().getSurname();

        final Pair<String, String> rootUserCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModelRootUser, secretKey);
        final String corporateIdRootUser = rootUserCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootUserEmail, secretKey);

        //create corporate #2 linked to the #1
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootUserEmail, secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight());

        //link Auth User to the Root User
        AdminService.linkUseridToCorporateSemi(new UserId(authUserId), corporateIdRootUser, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(secretKey, rootUserCorporate.getRight()).jsonPath().getString("rootUser.email");

        assertEquals(corporateUserLinkedEmail, corporateRootUserEmail);
    }

    @Test
    void LinkAuthUser_AuthUserDifferentName_Conflict() {
        //Root user #1 is created and email is verified
        final CreateCorporateModel createCorporateModelRootUser = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootUserEmail = createCorporateModelRootUser.getRootUser().getEmail();
        final String nameRootUser = createCorporateModelRootUser.getRootUser().getName();
        final String surnameRootUser = createCorporateModelRootUser.getRootUser().getSurname();

        final Pair<String, String> rootUserCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModelRootUser, secretKey);
        final String corporateIdRootUser = rootUserCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootUserEmail, secretKey);

        //create corporate #2 linked to the #1
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootUserEmail, secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser("test", "test", authUserCorporate.getRight());

        //link Auth User to the Root User
        AdminService.linkUseridToCorporateSemi(new UserId(authUserId), corporateIdRootUser, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("error", equalTo("USER_DATA_DO_NOT_MATCH"));
    }

    @Test
    void LinkAuthUser_LoginWithPassword_Success() {
        //Root user #1 is created and email is verified
        final CreateCorporateModel createCorporateModelRootUser = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootUserEmail = createCorporateModelRootUser.getRootUser().getEmail();
        final String nameRootUser = createCorporateModelRootUser.getRootUser().getName();
        final String surnameRootUser = createCorporateModelRootUser.getRootUser().getSurname();

        final Pair<String, String> rootUserCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModelRootUser, secretKey);
        final String corporateIdRootUser = rootUserCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootUserEmail, secretKey);

        //create corporate #2 linked to the #1
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootUserEmail, secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight());

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateIdRootUser);

        //success login with linked corporate using id from first corporate
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootUserEmail, new PasswordModel(PASSWORD));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(rootUserCorporate.getLeft()))
                .body("tokenType", equalTo("AUTH"));
    }

    @Test
    void LinkAuthUser_RootUserAccessToken_Success() {
        //Root user #1 is created and email is verified
        final CreateCorporateModel createCorporateModelRootUser = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootUserEmail = createCorporateModelRootUser.getRootUser().getEmail();
        final String nameRootUser = createCorporateModelRootUser.getRootUser().getName();
        final String surnameRootUser = createCorporateModelRootUser.getRootUser().getSurname();

        final Pair<String, String> rootUserCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModelRootUser, secretKey);
        final String corporateIdRootUser = rootUserCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootUserEmail, secretKey);

        //create corporate #2 linked to the #1
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootUserEmail, secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight());

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateIdRootUser);

        //get AuthToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootUserEmail, secretKey);

        //get accessToken
        final Identity identityModel = new Identity(new IdentityModel(corporateIdRootUser, IdentityType.CORPORATE));
        AuthenticationService.accessToken(identityModel, secretKey, authTokenAfterLinking)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(corporateIdRootUser))
                .body("identity.type", equalTo("CORPORATE"))
                .body("identity.id", equalTo(corporateIdRootUser));
    }

    @Test
    void LinkAuthUser_LinkedCorporateUserAccessToken_Success() {
        //Root user #1 is created and email is verified
        final CreateCorporateModel createCorporateModelRootUser = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootUserEmail = createCorporateModelRootUser.getRootUser().getEmail();
        final String nameRootUser = createCorporateModelRootUser.getRootUser().getName();
        final String surnameRootUser = createCorporateModelRootUser.getRootUser().getSurname();

        final Pair<String, String> rootUserCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModelRootUser, secretKey);
        final String corporateIdRootUser = rootUserCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootUserEmail, secretKey);

        //create corporate #2 linked to the #1
        final String linkedCorporateId = createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootUserEmail, secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight());

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateIdRootUser);

        //get AuthToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootUserEmail, secretKey);

        //get accessToken
        final Identity identityModel = new Identity(new IdentityModel(linkedCorporateId, IdentityType.CORPORATE));
        AuthenticationService.accessToken(identityModel, secretKey, authTokenAfterLinking)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(corporateIdRootUser))
                .body("identity.type", equalTo("CORPORATE"))
                .body("identity.id", equalTo(linkedCorporateId));
    }

    @Test
    void LinkAuthUser_AuthUserAccessToken_Success() {
        //Root user #1 is created and email is verified
        final CreateCorporateModel createCorporateModelRootUser = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootUserEmail = createCorporateModelRootUser.getRootUser().getEmail();
        final String nameRootUser = createCorporateModelRootUser.getRootUser().getName();
        final String surnameRootUser = createCorporateModelRootUser.getRootUser().getSurname();

        final Pair<String, String> rootUserCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModelRootUser, secretKey);
        final String corporateIdRootUser = rootUserCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootUserEmail, secretKey);

        //create corporate #2 linked to the #1
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootUserEmail, secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight());

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateIdRootUser);

        //get AuthToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootUserEmail, secretKey);

        //get accessToken
        final Identity identityModel = new Identity(new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE));
        AuthenticationService.accessToken(identityModel, secretKey, authTokenAfterLinking)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(corporateIdRootUser))
                .body("identity.type", equalTo("CORPORATE"))
                .body("identity.id", equalTo(authUserCorporate.getLeft()));
    }

    private static Pair<String, String> createCorporate() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        return CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
    }

    private static String createAuthUser(final String nameRootUser,
                                         final String surnameRootUser,
                                         final String corporateToken) {
        final UsersModel authUserModel = UsersModel.DefaultUsersModel()
                .setName(nameRootUser)
                .setSurname(surnameRootUser)
                .build();
        final Pair<String, String> authUser = UsersHelper.createAuthenticatedUser(authUserModel, secretKey, corporateToken);

        return authUser.getLeft();
    }

    private static void linkAuthUser(final String authUserId,
                                     final String corporateIdRootUser) {
        AdminService.linkUseridToCorporateSemi(new UserId(authUserId), corporateIdRootUser, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static String createLinkedCorporate(final String name,
                                                final String surname,
                                                final String corporateRootEmail,
                                                final String secretKey,
                                                final String corporatesProfileId) {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setSurname(surname)
                        .setName(name)
                        .setEmail(corporateRootEmail)
                        .build())
                .build();

        return CorporatesHelper.createKybVerifiedLinkedCorporate(createCorporateModel, secretKey);
    }
}
