package opc.junit.semi;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.admin.UserId;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.Identity;
import opc.models.shared.VerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class ScaSemiIdentitiesTests extends BaseSemiSetup {

    /**
     * SEMI (Single Email Multiple Identities) tests for AccessToken flow with all type of transactions.
     */
    private static final String OTP_CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";
    private static final String AUTHY_CHANNEL = EnrolmentChannel.AUTHY.name();
    private static final String BIOMETRIC_CHANNEL = EnrolmentChannel.BIOMETRIC.name();

    //TODO: to add cases for EnrolPushStepUp when semi innovator for ScaEnrolApp will be created.
    //private static String passcodeAppInnovatorToken;
    //private static String innovatorTokenScaEnrolApp;

    //@BeforeAll
    //public static void Setup(){
    //
    //    innovatorTokenScaEnrolApp = InnovatorHelper.loginInnovator(scaEnrolApp.getInnovatorEmail(),
    //            scaEnrolApp.getInnovatorPassword());
    //    enableAuthy(programmeIdScaEnrolApp, innovatorTokenScaEnrolApp);
    //    AdminService.setEnrolmentSca(adminToken, programmeIdScaEnrolApp, false, false, true);
    //    InnovatorService.updateProfileConstraint(
    //                    new PasswordConstraintsModel(PasswordConstraint.PASSCODE), programmeIdScaEnrolApp, innovatorTokenScaEnrolApp)
    //            .then()
    //            .statusCode(SC_NO_CONTENT);
    //}

    //@AfterAll
    //public static void TearDown(){
    //
    //    InnovatorService.updateProfileConstraint(
    //                    new PasswordConstraintsModel(PasswordConstraint.PASSWORD), scaEnrolApp.getProgrammeId(), innovatorTokenScaEnrolApp)
    //            .then()
    //            .statusCode(SC_NO_CONTENT);
    //}

    @Test
    public void Stepup_FirstIdentityWithOtpEnrolment_Success() {

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporate(createCorporateModel, corporateRootEmail);

        //create second linked identity
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //Start and verify StepUp with first identity
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void Stepup_SecondIdentityWithOtpEnrolment_Success() {

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporate(createCorporateModel, corporateRootEmail);

        //create second linked identity and get AccessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKey, corporatesProfileId);

        //Start and verify StepUp with second identity
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void StepupAuthUser_AuthUserWithOtpEnrolment_Success() {

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporate(createCorporateModel, corporateRootEmail);

        //create second linked identity
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(),
                secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token for authUser
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //Start and verify StepUp with AuthUser
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void StepupAuthUser_AuthUserStartStepupFirstIdentityVerify_NotFound() {

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporate(createCorporateModel, corporateRootEmail);

        //create second linked identity
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(),
                secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access tokens
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstIdentityAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //Start and verify StepUp with AuthUser
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, firstIdentityAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void Stepup_FirstIdentityStartStepupSecondIdentityVerify_NotFound() {

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporate(createCorporateModel, corporateRootEmail);

        //create second linked identity
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //Start StepUp with first identity
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //verify StepUp with second identity
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void IssuePushStepUp_FirstIdentityViaBiometric_Success() {

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //Issue StepUp with first identity
        AuthenticationService.issuePushStepup(BIOMETRIC_CHANNEL, passcodeAppSecretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void IssuePushStepUp_AuthUserViaBiometric_Success() {

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), passcodeAppSecretKey);

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token for authUser
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //Issue StepUp with first identity
        AuthenticationService.issuePushStepup(BIOMETRIC_CHANNEL, passcodeAppSecretKey, authUserAccessToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void IssuePushStepUp_LinkedIdentityViaAuthy_Success() {

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second linked corporate
        final String linkedCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKey, corporatesProfileId);

        //Issue StepUp with second (linked) identity
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, linkedCorporateAccessToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void IssuePushStepUp_AuthUserViaAuthy_Success() {

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second linked corporate
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(),
                secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token for authUser
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //Issue StepUp with second (linked) identity
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void IssuePushStepUp_AllIdentitiesViaAuthy_Success() {

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second linked corporate
        final String linkedCorporateAccessToken = createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(),
                secretKey, corporatesProfileId);

        //create corporate with Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);

        //link Auth User to the Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        final String rootUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //Issue StepUp with first identity
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, rootUserAccessToken)
                .then()
                .statusCode(SC_OK);

        //Issue StepUp with second (linked) identity
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, linkedCorporateAccessToken)
                .then()
                .statusCode(SC_OK);

        //Issue StepUp with authUser identity
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void IssuePushStepUp_AlreadyIssued_BadRequest() {

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second linked corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //Issue StepUp with first identity
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_OK);

        //Issue StepUp again with first identity
        // expected 200 because first try wasn't verify (Updated with DEV-2704)
        AuthenticationService.issuePushStepup(AUTHY_CHANNEL, secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_OK);
    }

    //TODO: to add cases for EnrolPushStepUp when semi innovator for ScaEnrolApp will be created.
    //@Test
    //public void EnrolPushStepUp_FirstIdentity_BadRequest() {
    //
    //    //Create first corporate identity enrolled for Biometric
    //    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaEnrolApp).build();
    //    final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
    //    final String name = createCorporateModel.getRootUser().getName();
    //    final String surname = createCorporateModel.getRootUser().getSurname();
    //
    //    final Pair<String, String> firstCorporate = createCorporate(createCorporateModel, corporateRootEmail);
    //
    //    //create second linked corporate
    //    createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
    //            secretKeyScaEnrolApp, corporateProfileIdScaEnrolApp);
    //
    //    //access token for the first Identity
    //    final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaEnrolApp);
    //    final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
    //                    new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
    //            secretKeyScaEnrolApp, corporateAuthenticationTokenAfterLinking);
    //
    //    //Verify StepUp with first identity
    //    AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, firstCorporateAccessToken);
    //
    //    //Enrol via Authy
    //    AuthenticationFactorsService.enrolPush(AUTHY_CHANNEL, secretKeyScaEnrolApp,
    //                    firstCorporateAccessToken)
    //            .then()
    //            .statusCode(SC_NO_CONTENT);
    //}

    private Pair<String, String> createCorporate(final CreateCorporateModel createCorporateModel,
                                                    final String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        return corporate;
    }

    private Pair<String, String> createCorporateAuthy(CreateCorporateModel createCorporateModel, String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());
        return corporate;
    }

    private Pair<String, String> createCorporateBiometric(final CreateCorporateModel createCorporateModel,
                                                          final String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, passcodeAppSecretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, passcodeAppSecretKey);
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey,
                passcodeAppSecretKey, corporate.getRight());
        return corporate;
    }

    private static String createLinkedCorporate(final String name,
                                                final String surname,
                                                final String corporateRootEmail,
                                                final String authToken,
                                                final String secretKey,
                                                final String corporatesProfileId) {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setSurname(surname)
                        .setName(name)
                        .setEmail(corporateRootEmail)
                        .build())
                .build();

        final String linkedIdentityId = CorporatesHelper.createKybVerifiedLinkedCorporate(createCorporateModel, secretKey);

        return AuthenticationHelper.requestAccessToken
                (new Identity(new IdentityModel(linkedIdentityId, IdentityType.CORPORATE)),
                        secretKey, authToken);
    }

    private static Pair<String, String> createAuthUserCorporate(final String corporatesProfileId,
                                                                final String secretKey) {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        return CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
    }

    private static String createAuthUser(final String nameRootUser,
                                         final String surnameRootUser,
                                         final String corporateToken,
                                         final String secretKey) {
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
}