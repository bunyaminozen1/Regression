package opc.junit.secure.biometric;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import commons.enums.State;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.secure.EnrolBiometricModel;
import opc.models.secure.LoginWithPasswordModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.simulator.AdditionalPropertiesModel;
import opc.models.simulator.TokenizeModel;
import opc.models.simulator.TokenizePropertiesModel;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.PasswordsService;
import opc.services.secure.SecureService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public abstract class AbstractLoginWithPasswordTests extends BaseBiometricSetup {

    protected abstract String getIdentityId();

    protected abstract String getIdentityToken();

    protected abstract String getIdentityEmail();

    protected abstract String getManagedAccountId();

    protected abstract IdentityType getIdentityType();

    protected abstract UserType getUserType();

    @Test
    public void LoginWithPassword_RootUser_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));
    }

    @Test
    public void LoginWithPassword_AuthUser_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getIdentityToken());
        final String userPassword = createPassword(user.getLeft());

        final String deviceId = enrolDeviceBiometric(user.getRight(), user.getLeft());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(userPassword))
                .setEmail(usersModel.getEmail())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(UserType.USER.name()))
                .body("credential.id", equalTo(user.getLeft()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));
    }

    @Test
    public void LoginWithPassword_LoginTwice_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        //verify auth factors
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void LoginWithPassword_TokenizedPassword_Success() {
        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());

        final String token =
                SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("token");

        final String tokenizedPassword =
                SimulatorService.tokenize(secretKey,
                                new TokenizeModel(new TokenizePropertiesModel(new AdditionalPropertiesModel(TestHelper.getDefaultPassword(secretKey), "PASSWORD"))), token)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("tokens.additionalProp1");

        AuthenticationService.loginWithPassword(new LoginModel(getIdentityEmail(), new PasswordModel(tokenizedPassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(getIdentityId()));
    }

    @Test
    public void LoginWithPassword_InvalidEmail_BadRequest() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, RandomStringUtils.randomAlphanumeric(10));
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_InvalidEmailFormat_BadRequest() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId,
                String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)));

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_UnknownEmail_Forbidden() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, UUID.randomUUID() + "@weavr.io");
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_EmptyDeviceId_BadRequest() {

        enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(null, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_EnrollDeviceAfterLogin_Success() {

        final String deviceId = RandomStringUtils.randomAlphanumeric(40);

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        //enroll device for Biometric
        final String random = SecureHelper.associate(getIdentityToken(), sharedKey);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(deviceId)
                .build();
        final String linking_code = SecureService.enrolDeviceBiometric(getIdentityToken(), sharedKey,
                        enrolBiometricModel)
                .jsonPath().getString("linkingCode");
        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linking_code);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());
    }

    @Test
    public void LoginWithPassword_UnknownPassword_Forbidden() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(10)))
                .setEmail(getIdentityEmail())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_ExpiredPassword_Forbidden() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());

        final String credentialsId = SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("credential.id");

        SimulatorHelper.simulateSecretExpiry(secretKey, credentialsId);

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void LoginWithPassword_NoPassword_BadRequest(final String password) {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(password))
                .setEmail(getIdentityEmail())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_NoPasswordModel_BadRequest() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(null)
                .setEmail(getIdentityEmail())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    /**
     * Scenario for DEV-4786: when two users can log in on the same device
     * Main steps:
     * 1. first user enrolled and login with password
     * 2. second user (not enrolled) login with password with same deviceId
     * Result:
     * - second user logged in
     * - second user stay not enrolled
     * - first user un-enrolled
     */
    @Test
    public void LoginWithPassword_TwoUsersSameDeviceRootUser_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //first user login
        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        //auth factors state before second user logged in
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo("ROOT"))
                .body("credential.id", equalTo(secondUser.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(secondUser.getLeft()))
                .body("tokenType", equalTo("ACCESS"));

        //verify auth factors for second user (user was not enrolled for any auth factors)
        assertEquals("{}", AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), secondUser.getRight())
                .then()
                .statusCode(SC_OK)
                .extract()
                .response()
                .asString());

        //first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void LoginWithPassword_TwoUsersSameDeviceFirstAuthUser_Success() {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getIdentityToken());
        final String userPassword = createPassword(user.getLeft());

        final String deviceId = enrolDeviceBiometric(user.getRight(), user.getLeft());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(userPassword))
                .setEmail(usersModel.getEmail())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(UserType.USER.name()))
                .body("credential.id", equalTo(user.getLeft()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        //auth factors state before second user logged in
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, user.getRight());

        //create second root user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo("ROOT"))
                .body("credential.id", equalTo(secondUser.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(secondUser.getLeft()))
                .body("tokenType", equalTo("ACCESS"));

        //verify auth factors for second user (user was not enrolled for any auth factors)
        assertEquals("{}", AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), secondUser.getRight())
                .then()
                .statusCode(SC_OK)
                .extract()
                .response()
                .asString());

        //first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void LoginWithPassword_TwoUsersSameDeviceSecondAuthUser_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //first root user login
        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        //auth factors state before second user logged in
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second root user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        //create authUser for second identity
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> authUser = UsersHelper.createAuthenticatedUser(usersModel, secretKey, secondUser.getRight());
        final String userPassword = createPassword(authUser.getLeft());

        // auth user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelAuthUser = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(userPassword))
                .setEmail(usersModel.getEmail())
                .setDeviceId(deviceId)
                .build();
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModelAuthUser)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(UserType.USER.name()))
                .body("credential.id", equalTo(authUser.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(secondUser.getLeft()))
                .body("tokenType", equalTo("ACCESS"));

        //verify auth factors for second user (user was not enrolled for any auth factors)
        assertEquals("{}", AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), authUser.getRight())
                .then()
                .statusCode(SC_OK)
                .extract()
                .response()
                .asString());

        //first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void LoginWithPassword_TwoUsersSameDeviceFirstUserWasNotLoggedIn_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //auth factors state before second user logged in
        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo("ROOT"))
                .body("credential.id", equalTo(secondUser.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(secondUser.getLeft()))
                .body("tokenType", equalTo("ACCESS"));

        //verify auth factors for second user (user was not enrolled for any auth factors)
        assertEquals("{}", AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), secondUser.getRight())
                .then()
                .statusCode(SC_OK)
                .extract()
                .response()
                .asString());

        //first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void LoginWithPassword_TwoUsersSameDeviceSecondUserEnrolled_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //first user login
        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureHelper.loginWithPassword(sharedKey, loginWithPasswordModel);

        //auth factors state before second user logged in
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same device
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);
        final String secondUserToken = SecureHelper.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity);

        //verify auth factors for second user (user was not enrolled for any auth factors)
        assertEquals("{}", AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), secondUserToken)
                .then()
                .statusCode(SC_OK)
                .extract()
                .response()
                .asString());

        //first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));

        //enroll device for Biometric after second user logged in
        final String randomSecondUser = SecureHelper.associate(secondUserToken, sharedKey);
        final EnrolBiometricModel enrolBiometricModelSecondUser = EnrolBiometricModel.builder()
                .random(randomSecondUser)
                .deviceId(deviceId)
                .build();
        final String linking_codeSecondUser = SecureService.enrolDeviceBiometric(secondUserToken, sharedKey,
                        enrolBiometricModelSecondUser)
                .jsonPath()
                .getString("linkingCode");

        SimulatorHelper.simulateEnrolmentLinking(secretKey, secondUser.getLeft(), linking_codeSecondUser);

        assertAuthFactorsState(secondUserToken, State.ACTIVE.name());
    }

    @Test
    public void LoginWithPassword_UseFirstUserTokenAfterSecondUserLogin_Success() {
        //Goal: verify that after second user logged in, the first user token no more works for challenges
        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //first user login
        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        final String firstUserToken = SecureHelper.loginWithPassword(sharedKey, loginWithPasswordModel);

        //send OWT and perform challenge before second user logged in
        final String owtBeforeSecondUserLoggedIn = sendOwt(firstUserToken);
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtBeforeSecondUserLoggedIn, "BIOMETRIC",
                        secretKey, firstUserToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);
        SecureHelper.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity);

        //send OWT and perform challenge after second user logged in
        final String owtAfterSecondUserLoggedIn = sendOwt(firstUserToken);
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtAfterSecondUserLoggedIn, "BIOMETRIC",
                        secretKey, firstUserToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        //verify first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void LoginWithPassword_UseSecondUserTokenForFirstUserChallenge_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //first user login
        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        final String firstUserToken = SecureHelper.loginWithPassword(sharedKey, loginWithPasswordModel);

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);
        final String secondUserToken = SecureHelper.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity);

        //send OWT with first user token
        final String owtAfterSecondUserLoggedIn = sendOwt(firstUserToken);

        //perform challenge with second user token
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtAfterSecondUserLoggedIn, "BIOMETRIC",
                        secretKey, secondUserToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void LoginWithPassword_FirstLogoutSecondLoginSameDevice_Success() {

        final String deviceId = enrolDeviceBiometric(getIdentityToken(), getIdentityId());

        //first user login
        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        final String firstUserToken = SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("token");

        //first user logout
        AuthenticationHelper.logout(firstUserToken, secretKey);

        //auth factors state before second user logged in
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        //create second user
        final CreateCorporateModel secondUserModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> secondUser = CorporatesHelper.createAuthenticatedCorporate(secondUserModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        final String secondUserEmail = secondUserModel.getRootUser().getEmail();

        // second user log in on the same devise
        final LoginWithPasswordModel loginWithPasswordModelSecondIdentity = getLoginWithPasswordModel(deviceId, secondUserEmail);

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModelSecondIdentity)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo("ROOT"))
                .body("credential.id", equalTo(secondUser.getLeft()))
                .body("identity.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("identity.id", equalTo(secondUser.getLeft()))
                .body("tokenType", equalTo("ACCESS"));

        //first user un-enrolled
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), getIdentityToken())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void LoginWithPassword_WithEmailHavingApostrophesOrSingleQuotes_Success(final String email){
        final UsersModel usersModel = UsersModel.DefaultUsersModel().setEmail(email).build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, getIdentityToken());
        final String userPassword = createPassword(user.getLeft());

        final String deviceId = enrolDeviceBiometric(user.getRight(), user.getLeft());

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(userPassword))
                .setEmail(usersModel.getEmail())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(UserType.USER.name()))
                .body("credential.id", equalTo(user.getLeft()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

    }

    @Test
    public void LoginWithPassword_RootUserEnrolledWithOtp_Success() {

        final String deviceId = SecureHelper.enrolBiometricWithOtpAndGetDeviceId(getIdentityToken(), getIdentityId(), passcodeApp);

        final LoginWithPasswordModel loginWithPasswordModel = getLoginWithPasswordModel(deviceId, getIdentityEmail());
        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.type", equalTo(getUserType().name()))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));
    }

    private LoginWithPasswordModel getLoginWithPasswordModel(final String deviceId, String userEmail) {
        return LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                .setEmail(userEmail)
                .setDeviceId(deviceId)
                .build();
    }

    private String enrolDeviceBiometric(final String authenticationToken,
                                        final String identityId) {

        final String random = SecureHelper.associate(authenticationToken, sharedKey);
        final String deviceId = RandomStringUtils.randomAlphanumeric(40);

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(deviceId)
                .build();

        final String linking_code = SecureService.enrolDeviceBiometric(authenticationToken, sharedKey,
                        enrolBiometricModel)
                .jsonPath().getString("linkingCode");

        SimulatorHelper.simulateEnrolmentLinking(secretKey, identityId, linking_code);

        return deviceId;
    }

    private String createPassword(final String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }

    private String sendOwt(final String userToken) {
        return OutgoingWireTransfersHelper
                .sendOwt(outgoingWireTransfersProfileId, getManagedAccountId(), new CurrencyAmount(Currency.EUR.name(), 100L),
                        secretKey, userToken)
                .getLeft();
    }

    /**
     * This method assert authentication factors status of an identity
     */
    private void assertAuthFactorsState(final String identityToken,
                                        final String expectedStatus) {

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), identityToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("factors[0].status").equals(expectedStatus),
                Optional.of(String.format("Expecting 200 with an authentication factor in state %s, check logged payload", expectedStatus)));
    }

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
