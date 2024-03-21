package opc.junit.semi;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.shared.Identity;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class CorporateLoginSemiTests extends BaseSemiSetup {
    /**
     * SEMI (Single Email Multiple Identities) tests for LoginWithPassword flow.
     */
    private static final String IDENTITY_TYPE = IdentityType.CORPORATE.name();

    @Test
    public void LoginWithPassword_UserBelongToMoreIdentities_Success() {
        //Creating 2nd corporate - linking with first one
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String identityId = authenticatedCorporate.getLeft();
        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);

        final CorporateRootUserModel rootUser = CorporateRootUserModel.DefaultRootUserModel()
                .setSurname(surname)
                .setName(name)
                .setEmail(corporateRootEmail)
                .build();

        final CreateCorporateModel createCorporateToLink = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(rootUser)
                .build();

        final String identityIdLinked = CorporatesHelper.createCorporate(createCorporateToLink, secretKey);

        CorporatesHelper.verifyKyb(secretKey, identityIdLinked);
        final String corporateRootEmailLinked = createCorporateToLink.getRootUser().getEmail();

        //success login with linked corporate using id from first corporate
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));

        //success login with linked corporate using id from second corporate
        final LoginModel loginModelWithSecondIdentity = new LoginModel(corporateRootEmailLinked, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithSecondIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));
    }

    @Test
    public void LoginWithPassword_UserWithSamePassword_Conflict() {
        //Root user is created and login with a password
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String identityId = authenticatedCorporate.getLeft();
        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);

        final CorporateRootUserModel rootUser = CorporateRootUserModel.DefaultRootUserModel()
                .setSurname(surname)
                .setName(name)
                .setEmail(corporateRootEmail)
                .build();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(IDENTITY_TYPE))
                .body("identity.id", equalTo(identityId))
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId));

        //New identity linked to root user with the same email, name and surname
        final CreateCorporateModel createCorporateToLink = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(rootUser)
                .build();

        final String identityLinkedId = CorporatesHelper.createCorporate(createCorporateToLink, secretKey);
        final String corporateLinkedRootEmail = createCorporateToLink.getRootUser().getEmail();

        //The same password created for linked identity
        PasswordsService.createPassword(createPasswordModel, identityLinkedId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_ALREADY_CREATED"));

        //Login with new identity
        final LoginModel loginModelWithLinkedIdentity = new LoginModel(corporateLinkedRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithLinkedIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));
    }

    @Test
    public void ResumeLostPassword_SemiLinkedCorporate_Success() {
        //Root user is created and login with a password
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String identityId = authenticatedCorporate.getLeft();
        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);

        final CorporateRootUserModel rootUser = CorporateRootUserModel.DefaultRootUserModel()
                .setSurname(surname)
                .setName(name)
                .setEmail(corporateRootEmail)
                .build();

        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId));

        //New identity linked to root user with the same email, name and surname
        final CreateCorporateModel createCorporateToLink = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(rootUser)
                .build();
        CorporatesHelper.createCorporate(createCorporateToLink, secretKey);

        final String corporateLinkedRootEmail = createCorporateToLink.getRootUser().getEmail();

        //Start lost password flow for linked identity
        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateLinkedRootEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(corporateLinkedRootEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        //Login with new identity
        final LoginModel loginModelWithLinkedIdentity = new LoginModel(corporateLinkedRootEmail, new PasswordModel("NewPass1234"));
        AuthenticationService.loginWithPassword(loginModelWithLinkedIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId));

    }

    @Test
    public void AccessToken_LinkedUserCanUseOnlyAccessToken_Success() {
        //corporate created in beforeEach - classic login with checking tokenType
        //Root user is created and login with a password
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String identityId = authenticatedCorporate.getLeft();
        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);

        final CorporateRootUserModel rootUser = CorporateRootUserModel.DefaultRootUserModel()
                .setSurname(surname)
                .setName(name)
                .setEmail(corporateRootEmail)
                .build();

        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("ACCESS"));

        //create second corporate and check tokenType
        final CreateCorporateModel createCorporateToLink = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(rootUser)
                .build();

        final String identityIdLinked = CorporatesHelper.createCorporate(createCorporateToLink, secretKey);
        final String corporateLinkedRootEmail = createCorporateToLink.getRootUser().getEmail();

        CorporatesHelper.verifyKyb(secretKey, identityIdLinked);
        AuthenticationService.loginWithPassword(new LoginModel(corporateLinkedRootEmail, new PasswordModel(corporatePassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));

        final String corporateToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //since identities are linked, corporate token (authToken) is valid only for AccessToken endpoint
        //any actions with other endpoints shouldn't be allowed

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateToLink.getBaseCurrency()).build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporateToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
        final Identity identity = new Identity(new IdentityModel(identityIdLinked, IdentityType.CORPORATE));

        AuthenticationService.accessToken(identity, secretKey, corporateToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(identityId))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(identityIdLinked))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());

        final String accessToken = AuthenticationHelper.requestAccessToken(identity, secretKey, corporateToken);

        ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, createCorporateToLink.getBaseCurrency(), secretKey, accessToken);
    }


}

